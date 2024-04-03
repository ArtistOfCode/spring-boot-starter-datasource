package com.codeartist.component.datasource.context;

import com.codeartist.component.datasource.bean.DataSourceMultiProperties;
import com.codeartist.component.datasource.bean.MybatisPlusConfigurationBean;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.util.CollectionUtils;

import javax.sql.DataSource;
import java.util.function.Function;

/**
 * 数据库多数据连接注册Bean
 *
 * @author AiJiangnan
 * @date 2023-11-14
 */
@Slf4j
public class DataSourceMultiRegister implements ImportBeanDefinitionRegistrar, EnvironmentAware, BeanFactoryAware {

    private static final String SPRING_DATASOURCE_PREFIX = "spring.datasource";

    private BeanFactory beanFactory;
    private Environment environment;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        BindResult<DataSourceMultiProperties> bindResult =
                Binder.get(environment).bind(SPRING_DATASOURCE_PREFIX, DataSourceMultiProperties.class);

        if (!bindResult.isBound()) {
            return;
        }

        DataSourceMultiProperties multiProperties = bindResult.get();

        if (CollectionUtils.isEmpty(multiProperties.getMulti())) {
            return;
        }

        multiProperties.getMulti().forEach((name, properties) -> {
            registerBean(registry, name, DataSource.class, n -> instanceDataSource(properties));
            registerBean(registry, name, SqlSessionFactory.class, this::instanceSqlSessionFactory);
            registerBean(registry, name, SqlSessionTemplate.class, this::instanceSqlSessionTemplate);
            registerBean(registry, name, DataSourceTransactionManager.class, this::instanceDataSourceTransactionManager);
        });
    }

    private DataSource instanceDataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    private SqlSessionFactory instanceSqlSessionFactory(String name) {
        MybatisPlusConfigurationBean configurationBean = beanFactory.getBean(MybatisPlusConfigurationBean.class);
        DataSource dataSource = beanFactory.getBean(name + DataSource.class.getSimpleName(), DataSource.class);
        try {
            return configurationBean.getSqlSessionFactory(dataSource).getObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private SqlSessionTemplate instanceSqlSessionTemplate(String name) {
        MybatisPlusConfigurationBean configurationBean = beanFactory.getBean(MybatisPlusConfigurationBean.class);
        SqlSessionFactory sqlSessionFactory = beanFactory
                .getBean(name + SqlSessionFactory.class.getSimpleName(), SqlSessionFactory.class);
        return configurationBean.getSqlSessionTemplate(sqlSessionFactory);
    }

    private DataSourceTransactionManager instanceDataSourceTransactionManager(String name) {
        DataSource dataSource = beanFactory.getBean(name + DataSource.class.getSimpleName(), DataSource.class);
        return new DataSourceTransactionManager(dataSource);
    }

    private <T> void registerBean(BeanDefinitionRegistry registry, String name, Class<T> beanClass,
                                  Function<String, T> instanceFunction) {

        String beanName = name + beanClass.getSimpleName();

        BeanDefinition definition = BeanDefinitionBuilder.genericBeanDefinition(beanClass, () -> instanceFunction.apply(name))
                .setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_NAME)
                .getRawBeanDefinition();

        registry.registerBeanDefinition(beanName, definition);
        log.info("Bean '{}' of type [{}] is registered", beanName, beanClass.getName());
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}
