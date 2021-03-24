package com.newegg.mkpl.radial.datasource;

/**
 * 可切换数据源，请保证整个BeanFactory里面只含有一个数据源，这样对于其他依赖于单一数据源的自动装配框架才不会抛出错误。
 *
 * @author mm92
 * @since 1.0.0 2018-11-09
 */
public class SwitchableDataSource extends AbstractSwitchableDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        return DataSourceContextHolder.getDataSourceName();
    }

}
