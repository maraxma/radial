package com.newegg.mkpl.radial.property;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 多数据源配置承载器。
 *
 * @author mm92
 * @since 1.0.0 2018-11-09
 */
@ConfigurationProperties(prefix = "radial")
public class MultiDataSourceProperties {

    private Map<String, RadialDataSourceProperties> datasources;

    public Map<String, RadialDataSourceProperties> getDatasources() {
        return datasources;
    }

    public void setDatasources(Map<String, RadialDataSourceProperties> datasources) {
        this.datasources = datasources;
    }

}
