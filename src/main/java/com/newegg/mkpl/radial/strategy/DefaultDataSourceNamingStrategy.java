package com.newegg.mkpl.radial.strategy;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;

/**
 * 默认的数据源命名策略。
 * <p>它的命名策略为："dataSource_" + dataSourceTitle;</p>
 *
 * @author mm92
 * @since 1.0.0 2018-11-09
 */
public class DefaultDataSourceNamingStrategy implements DataSourceNamingStrategy {

    private static final String PREFIX = "dataSource";

    @Override
    public String getName(String dataSourceTitle, DataSourceProperties dataSourceProperties) {
        return PREFIX + "_" + dataSourceTitle;
    }

}
