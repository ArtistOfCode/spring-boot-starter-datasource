package com.codeartist.component.datasource.bean;

import lombok.Data;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * 数据库多数据源配置类
 *
 * @author AiJiangnan
 * @date 2024/4/2
 */
@Data
@ConfigurationProperties(prefix = "spring.datasource")
public class DataSourceMultiProperties {

    private Map<String, DataSourceProperties> multi;
}
