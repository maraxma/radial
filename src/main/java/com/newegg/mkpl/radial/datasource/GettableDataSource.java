package com.newegg.mkpl.radial.datasource;

import java.util.Map;

import javax.sql.DataSource;

/**
 * 可获取数据源接口
 *
 * @author mm92
 * @since 1.0.0 2018-11-10
 */
public interface GettableDataSource {

    /**
     * 获得一个数据源
     *
     * @param dataSourceName 数据源名称
     * @return 数据源，若无法找到则抛出DataSourceNotFoundException
     */
    DataSource getDataSource(Object dataSourceName);

    /**
     * 获得当前的数据源
     *
     * @return 数据源，若无法找到则抛出DataSourceNotFoundException
     */
    DataSource getCurrentDataSource();

    /**
     * 获得数据源的名称
     *
     * @param dataSource 数据源
     * @return 数据源名称，可能为null
     */
    String getDataSourceName(DataSource dataSource);

    /**
     * 获得所有的数据源
     *
     * @return 所有的数据源，请返回一个不可变的Map，可以是空Map，但不能为null
     */
    Map<String, DataSource> getAllDataSources();

    /**
     * 获得默认的数据源
     *
     * @return 默认数据源，若无法找到则抛出DataSourceNotFoundException
     */
    DataSource getDefaultDataSource();

}
