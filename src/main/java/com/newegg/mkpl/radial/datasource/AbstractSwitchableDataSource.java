package com.newegg.mkpl.radial.datasource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.sql.DataSource;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.jdbc.datasource.lookup.DataSourceLookup;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import com.alibaba.druid.pool.DruidDataSource;
import com.newegg.mkpl.radial.exception.DataSourceNotFoundException;
import com.newegg.mkpl.radial.util.AopTargetUtils;
import com.zaxxer.hikari.HikariDataSource;

/**
 * 抽象可切换数据源。为了实现更多功能，它的源码部分拷贝自spring中的AbstractRoutingDataSource，并且将部分不常用的公开方法屏蔽了。
 * <p>为什么拷贝了AbstractRoutingDataSource的源码而不是直接继承它以扩展功能？<br/>
 * 答：AbstractRoutingDataSource源码里面的成员变量大都是私有的且没有提供公有的Getter，因此继承不能实现目的功能。
 * </p>
 *
 * @author mm92
 * @since 2018-11-10
 */
public abstract class AbstractSwitchableDataSource extends AbstractDataSource implements GettableDataSource, SwitchableDataSourceMXBean, InitializingBean, DisposableBean {

    @Nullable
    private Map<Object, Object> targetDataSources;

    @Nullable
    private Object defaultTargetDataSource;

    private DataSourceLookup dataSourceLookup = new JndiDataSourceLookup();

    @Nullable
    private Map<Object, DataSource> resolvedDataSources;

    @Nullable
    private DataSource resolvedDefaultDataSource;


    /**
     * 设置可切换数据源备选项
     *
     * @param targetDataSources 数据源集合
     */
    public void setTargetDataSources(Map<Object, Object> targetDataSources) {
        this.targetDataSources = targetDataSources;
    }

    /**
     * 设置默认的数据源（必须）
     *
     * @param defaultTargetDataSource 默认数据源
     */
    public void setDefaultTargetDataSource(Object defaultTargetDataSource) {
        this.defaultTargetDataSource = defaultTargetDataSource;
    }

    /**
     * Set the DataSourceLookup implementation to use for resolving data source
     * name Strings in the {@link #setTargetDataSources targetDataSources} map.
     * <p>Default is a {@link JndiDataSourceLookup}, allowing the JNDI names
     * of application server DataSources to be specified directly.
     */
    protected void setDataSourceLookup(@Nullable DataSourceLookup dataSourceLookup) {
        this.dataSourceLookup = (dataSourceLookup != null ? dataSourceLookup : new JndiDataSourceLookup());
    }


    @Override
    public void afterPropertiesSet() {
        if (this.targetDataSources == null) {
            throw new IllegalArgumentException("Property 'targetDataSources' is required");
        }
        this.resolvedDataSources = new HashMap<>(this.targetDataSources.size());
        this.targetDataSources.forEach((key, value) -> {
            Object lookupKey = resolveSpecifiedLookupKey(key);
            DataSource dataSource = resolveSpecifiedDataSource(value);
            this.resolvedDataSources.put(lookupKey, dataSource);
        });
        if (this.defaultTargetDataSource != null) {
            this.resolvedDefaultDataSource = resolveSpecifiedDataSource(this.defaultTargetDataSource);
        }
    }

    /**
     * Resolve the given lookup key object, as specified in the
     * {@link #setTargetDataSources targetDataSources} map, into
     * the actual lookup key to be used for matching with the
     * {@link #determineCurrentLookupKey() current lookup key}.
     * <p>The default implementation simply returns the given key as-is.
     *
     * @param lookupKey the lookup key object as specified by the user
     * @return the lookup key as needed for matching
     */
    protected Object resolveSpecifiedLookupKey(Object lookupKey) {
        return lookupKey;
    }

    /**
     * Resolve the specified data source object into a DataSource instance.
     * <p>The default implementation handles DataSource instances and data source
     * names (to be resolved via a {@link #setDataSourceLookup DataSourceLookup}).
     *
     * @param dataSource the data source value object as specified in the
     *                   {@link #setTargetDataSources targetDataSources} map
     * @return the resolved DataSource (never {@code null})
     * @throws IllegalArgumentException in case of an unsupported value type
     */
    protected DataSource resolveSpecifiedDataSource(Object dataSource) throws IllegalArgumentException {
        if (dataSource instanceof DataSource) {
            return (DataSource) dataSource;
        } else if (dataSource instanceof String) {
            return this.dataSourceLookup.getDataSource((String) dataSource);
        } else {
            throw new IllegalArgumentException(
                    "Illegal data source value - only [javax.sql.DataSource] and String supported: " + dataSource);
        }
    }


    @Override
    public Connection getConnection() throws SQLException {
        try {
            return determineTargetDataSource().getConnection();
        } catch (Exception e) {
            throw new SQLException("Cannot initialize connection from current pointed datasource [" + determineCurrentLookupKey() + "]", e);
        }
    }

    @Override
    public Map<String, PooledDataSourceInfo> getPooledDataSourceInfo() {
        Map<String, DataSource> dataSources = getAllDataSources();
        // 目前仅支持Druid和HikariCP的判定
        final boolean hikariLoaded = ClassUtils.isPresent("com.zaxxer.hikari.HikariDataSource", Thread.currentThread().getContextClassLoader());
        final boolean druidLoaded = ClassUtils.isPresent("com.alibaba.druid.pool.DruidDataSource", Thread.currentThread().getContextClassLoader());
        return dataSources.entrySet().stream().collect(HashMap::new, (l, r) -> {
            String dataSourceName = r.getKey();
            DataSource ds = r.getValue();
            if (hikariLoaded && ds instanceof HikariDataSource) {
                l.put(dataSourceName, new PooledDataSourceInfo(
                        ds.getClass().getName(),
                        ((HikariDataSource) ds).isRunning(),
                        ((HikariDataSource) ds).getHikariPoolMXBean() == null ? -1 : ((HikariDataSource) ds).getHikariPoolMXBean().getActiveConnections(),
                        ((HikariDataSource) ds).getHikariPoolMXBean() == null ? -1 : ((HikariDataSource) ds).getHikariPoolMXBean().getTotalConnections()));
            } else if (druidLoaded && r.getValue() instanceof DruidDataSource) {
                l.put(dataSourceName, new PooledDataSourceInfo(
                        ds.getClass().getName(),
                        !((DruidDataSource) ds).isClosed(),
                        ((DruidDataSource) ds).getActiveCount(),
                        ((DruidDataSource) ds).getPoolingCount()));
            }
        }, HashMap::putAll);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return determineTargetDataSource().getConnection(username, password);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return (T) this;
        }
        return determineTargetDataSource().unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return (iface.isInstance(this) || determineTargetDataSource().isWrapperFor(iface));
    }

    /**
     * Retrieve the current target DataSource. Determines the
     * {@link #determineCurrentLookupKey() current lookup key}, performs
     * a lookup in the {@link #setTargetDataSources targetDataSources} map,
     * falls back to the specified
     * {@link #setDefaultTargetDataSource default target DataSource} if necessary.
     *
     * @see #determineCurrentLookupKey()
     */
    protected DataSource determineTargetDataSource() {
        Assert.notNull(this.resolvedDataSources, "DataSource router not initialized");
        Object lookupKey = determineCurrentLookupKey();
        DataSource dataSource;
        if (lookupKey == null || (lookupKey instanceof String && "".equals(((String) lookupKey).trim()))) {
            dataSource = this.resolvedDefaultDataSource;
        } else {
            dataSource = this.resolvedDataSources.get(lookupKey);
            if (dataSource == null) {
                throw new IllegalStateException("Cannot find DataSource named [" + lookupKey + "], please check your codes or configuration files");
            }
        }
        return dataSource;
    }

    @Override
    public DataSource getDataSource(Object dataSourceName) {
        if (resolvedDataSources != null) {
            DataSource ds = resolvedDataSources.get(dataSourceName);
            if (ds != null) {
                return ds;
            }
        }
        throw new DataSourceNotFoundException("Cannot find datasource: " + dataSourceName + ", current available datasources are " + getAvailableDataSourceNames());
    }

    @Override
    public DataSource getCurrentDataSource() {
        if (resolvedDefaultDataSource == null) {
            throw new DataSourceNotFoundException("Cannot find default datasource");
        }
        return resolvedDefaultDataSource;
    }

    @Override
    public String getCurrentDataSourceName() {
        Object name = determineCurrentLookupKey();
        if (name == null) {
            throw new DataSourceNotFoundException("Cannot find current datasource and it's name");
        }
        return String.valueOf(name);
    }

    @Override
    public String getDataSourceName(DataSource dataSource) {
        for (Map.Entry<Object, DataSource> en : Objects.requireNonNull(resolvedDataSources).entrySet()) {
            if (en.getValue().equals(dataSource) || AopTargetUtils.getTarget(en.getValue()).equals(dataSource)) {
                return String.valueOf(en.getKey());
            }
        }
        return null;
    }

    @Override
    public Map<String, DataSource> getAllDataSources() {
        return Collections.unmodifiableMap(resolvedDataSources == null
                ? new HashMap<>()
                : resolvedDataSources.entrySet().stream().collect(HashMap::new, (l, r) -> l.put(String.valueOf(r.getKey()), r.getValue()), HashMap::putAll));
    }

    @Override
    public DataSource getDefaultDataSource() {
        if (resolvedDefaultDataSource == null) {
            throw new DataSourceNotFoundException("Cannot find default datasource and its name");
        }
        return resolvedDefaultDataSource;
    }

    @Override
    public String getDefaultDataSourceName() {
        return getDataSourceName(getDefaultDataSource());
    }

    @Override
    public Set<String> getAvailableDataSourceNames() {
        if (resolvedDataSources == null) {
            return new HashSet<>();
        }
        return resolvedDataSources
                .entrySet().stream()
                .collect(HashSet::new, (l, r) -> l.add(String.valueOf(r.getKey())), HashSet::addAll);
    }

    @Override
    public boolean containsDataSource(String dataSourceName) {
        return getAllDataSources().containsKey(dataSourceName);
    }

    @Override
    public void destroy() throws Exception {
        closeDataSource(resolvedDefaultDataSource);
        for (Map.Entry<Object, DataSource> en : Objects.requireNonNull(resolvedDataSources).entrySet()) {
            closeDataSource(en.getValue());
        }
    }

    private void closeDataSource(DataSource dataSource) throws Exception {
        if (dataSource instanceof AutoCloseable) {
            ((AutoCloseable) dataSource).close();
        }
    }

    /**
     * 重要方法，重写此方法以实现数据源切换
     *
     * @return 返回的值将作为每次取DataSource的key
     */
    @Nullable
    protected abstract Object determineCurrentLookupKey();

    public Map<Object, DataSource> getResolvedDataSources() {
        return resolvedDataSources;
    }

}
