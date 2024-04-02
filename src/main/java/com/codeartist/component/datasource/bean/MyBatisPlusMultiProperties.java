package com.codeartist.component.datasource.bean;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusProperties;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * MyBatisPlus多数据源配置类
 *
 * @author AiJiangnan
 * @date 2024/4/2
 */
@Data
@ConfigurationProperties(prefix = Constants.MYBATIS_PLUS)
public class MyBatisPlusMultiProperties {

    private Map<String, MybatisPlusProperties> datasource;
}
