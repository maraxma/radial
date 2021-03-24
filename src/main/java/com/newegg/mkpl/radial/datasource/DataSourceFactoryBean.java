package com.newegg.mkpl.radial.datasource;

import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertyNameAliases;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

/**
 * 数据源工厂Bean，用于生产数据源
 *
 * @author mm92
 * @since 1.0.0 2018-11-08
 */
public class DataSourceFactoryBean implements FactoryBean<DataSource>, DisposableBean {

    private final DataSourceProperties dataSourceProperties;
    private final Map<String, Object> detailsProp;

    private DataSource instance = null;

    private final static ConfigurationPropertyNameAliases ALIASES = new ConfigurationPropertyNameAliases(); //别名

    static {
        //由于部分数据源配置不同，所以在此处添加别名，避免切换数据源出现某些参数无法注入的情况
        ALIASES.addAliases("url", "jdbc-url");
        ALIASES.addAliases("username", "user");
    }

    public DataSourceFactoryBean(DataSourceProperties dataSourceProperties, Map<String, Object> detailsProp) {
        this.dataSourceProperties = dataSourceProperties;
        this.detailsProp = detailsProp;
    }

    private void bind(DataSource result, Map<String, Object> properties) {
        ConfigurationPropertySource source = new MapConfigurationPropertySource(properties);
        Binder binder = new Binder(source.withAliases(ALIASES));
        binder.bind(ConfigurationPropertyName.EMPTY, Bindable.ofInstance(result));  //将参数绑定到对象
    }

    @Override
    public Class<?> getObjectType() {
        return DataSource.class;
    }

    @Override
    public DataSource getObject() {
        DataSource ds = dataSourceProperties.initializeDataSourceBuilder().type(dataSourceProperties.getType()).build();
        if (detailsProp != null) {
            bind(ds, detailsProp);
        }
        instance = ds;
        return ds;
    }

    @Override
    public void destroy() throws Exception {
        if (instance != null) {
            if (instance instanceof AutoCloseable) {
                ((AutoCloseable) instance).close();
            }
        }
    }
}
