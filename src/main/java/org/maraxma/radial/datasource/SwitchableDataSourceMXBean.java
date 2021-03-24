package org.maraxma.radial.datasource;

import java.util.Map;
import java.util.Set;

/**
 * 可切换数据源MXBean。
 * <p>这个MXBean可以帮助你在运行时观看一些参数或者执行一些函数。</p>
 *
 * @author mm92
 * @since 1.2.0 2019-03-19
 */
public interface SwitchableDataSourceMXBean {
    /**
     * 获得默认数据源的名称
     *
     * @return 默认数据源名称，若无法找到则抛出DataSourceNotFoundException
     */
    String getDefaultDataSourceName();

    /**
     * 获得有效的数据源名称列表
     *
     * @return 名称列表，不可能为null，可以为空
     */
    Set<String> getAvailableDataSourceNames();

    /**
     * 是否包含指定的DataSource
     *
     * @param dataSourceName 数据源名称
     * @return 包含则返回true，否则返回false
     */
    boolean containsDataSource(String dataSourceName);

    /**
     * 获得当前的数据源名称（这个名称可能会一直变化）。
     *
     * @return 当前数据源名称
     */
    String getCurrentDataSourceName();

    /**
     * 获得当前所拥有的池化数据源的状态信息。
     *
     * @return 池化数据源信息表，若全部都不是池化数据源，请返回空列表
     */
    Map<String, PooledDataSourceInfo> getPooledDataSourceInfo();
}
