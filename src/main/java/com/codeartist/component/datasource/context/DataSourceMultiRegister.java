package com.codeartist.component.datasource.context;

import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.codeartist.component.datasource.bean.DataSourceMultiProperties;
import com.codeartist.component.datasource.bean.MybatisPlusConfigurationBean;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
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

/**
 * 数据库多数据连接注册Bean
 *
 * @author AiJiangnan
 * @date 2023-11-14
 */
@Slf4j
public class DataSourceMultiRegister implements ImportBeanDefinitionRegistrar, EnvironmentAware, BeanFactoryAware {

    private static final String SPRING_DATASOURCE_PREFIX = "spring.datasource";
    private static final String DATASOURCE_BEAN_NAME = "DataSource";
    private static final String SQL_SESSION_FACTORY_BEAN_NAME = "SqlSessionFactory";
    private static final String SQL_SESSION_TEMPLATE_BEAN_NAME = "SqlSessionTemplate";
    private static final String TRANSACTION_MANAGER_BEAN_NAME = "TransactionManager";

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
            registerDataSourceBean(registry, name, properties);
            registerSqlSessionFactoryBean(registry, name);
            registerSqlSessionTemplateBean(registry, name);
            registerTransactionManagerBean(registry, name);
        });
    }

    private void registerDataSourceBean(BeanDefinitionRegistry registry, String name, DataSourceProperties properties) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(DataSource.class, () ->
                properties.initializeDataSourceBuilder().type(HikariDataSource.class).build());
        AbstractBeanDefinition definition = builder.getRawBeanDefinition();
        definition.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_NAME);
        registry.registerBeanDefinition(name + DATASOURCE_BEAN_NAME, definition);
        printRegisterBeanLog(name + DATASOURCE_BEAN_NAME, DataSource.class.getName());
    }

    private void registerSqlSessionFactoryBean(BeanDefinitionRegistry registry, String name) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(MybatisSqlSessionFactoryBean.class, () -> {
            MybatisPlusConfigurationBean configurationBean = beanFactory.getBean(MybatisPlusConfigurationBean.class);
            DataSource dataSource = beanFactory.getBean(name + DATASOURCE_BEAN_NAME, DataSource.class);
            return configurationBean.getSqlSessionFactory(dataSource);
        });
        AbstractBeanDefinition definition = builder.getRawBeanDefinition();
        definition.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_NAME);
        registry.registerBeanDefinition(name + SQL_SESSION_FACTORY_BEAN_NAME, definition);
        printRegisterBeanLog(name + SQL_SESSION_FACTORY_BEAN_NAME, MybatisSqlSessionFactoryBean.class.getName());
    }

    private void registerSqlSessionTemplateBean(BeanDefinitionRegistry registry, String name) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(SqlSessionTemplate.class, () -> {
            MybatisPlusConfigurationBean configurationBean = beanFactory.getBean(MybatisPlusConfigurationBean.class);
            SqlSessionFactory sqlSessionFactory = beanFactory.getBean(name + SQL_SESSION_FACTORY_BEAN_NAME, SqlSessionFactory.class);
            return configurationBean.getSqlSessionTemplate(sqlSessionFactory);
        });
        AbstractBeanDefinition definition = builder.getRawBeanDefinition();
        definition.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_NAME);
        registry.registerBeanDefinition(name + SQL_SESSION_TEMPLATE_BEAN_NAME, definition);
        printRegisterBeanLog(name + SQL_SESSION_TEMPLATE_BEAN_NAME, MybatisSqlSessionFactoryBean.class.getName());
    }

    private void registerTransactionManagerBean(BeanDefinitionRegistry registry, String name) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(DataSourceTransactionManager.class, () -> {
            DataSource dataSource = beanFactory.getBean(name + DATASOURCE_BEAN_NAME, DataSource.class);
            return new DataSourceTransactionManager(dataSource);
        });
        AbstractBeanDefinition definition = builder.getRawBeanDefinition();
        definition.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_NAME);
        registry.registerBeanDefinition(name + TRANSACTION_MANAGER_BEAN_NAME, definition);
        printRegisterBeanLog(name + TRANSACTION_MANAGER_BEAN_NAME, DataSourceTransactionManager.class.getName());
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    private void printRegisterBeanLog(String beanName, String type) {
        log.info("Bean '{}' of type [{}] is registered", beanName, type);
    }
}
