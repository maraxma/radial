package org.maraxma.radial.strategy;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;

/**
 * 数据源命名策略接口。实现此接口并将实现放入spring容器中即可更改命名策略。
 *
 * @author mm92
 * @since 1.0.0 2018-11-09
 */
public interface DataSourceNamingStrategy {

    /**
     * 命名方法
     *
     * @param dataSourceTitle      你在配置中的数据源的名称
     * @param dataSourceProperties 数据源的其他配置信息
     * @return 返回的这个String将作为每个数据源的名称
     */
    String getName(String dataSourceTitle, DataSourceProperties dataSourceProperties);

}
