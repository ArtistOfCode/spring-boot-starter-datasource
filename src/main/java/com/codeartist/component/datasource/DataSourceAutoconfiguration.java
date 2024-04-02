package com.codeartist.component.datasource;

import com.codeartist.component.datasource.bean.MybatisPlusConfigurationBean;
import com.codeartist.component.datasource.context.DataSourceMultiRegister;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * 数据源配置
 *
 * @author AiJiangnan
 * @date 2024/4/2
 */
@SpringBootConfiguration
@Import(DataSourceMultiRegister.class)
public class DataSourceAutoconfiguration {

    @Bean
    public MybatisPlusConfigurationBean mybatisPlusConfigurationBean() {
        return new MybatisPlusConfigurationBean();
    }
}
