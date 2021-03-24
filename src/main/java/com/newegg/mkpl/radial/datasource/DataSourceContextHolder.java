package com.newegg.mkpl.radial.datasource;

/**
 * 数据源上下文保持器。它内部保持的dataSource决定了当前后文使用的数据源，若你在@UseDataSource无法控制的地方需要切换数据源，
 * 请使用它的setDataSourceName()静态方法，他可以帮助你使用硬编码灵活切换数据源。
 * <p>这个数据源上下文保持器仅在使用可切换数据源（@EnableSwitchableDataSource）时有用。</p>
 *
 * @author mm92
 * @since 1.0.0 2018-11-09
 */
public class DataSourceContextHolder {

    private static final ThreadLocal<String> CONTEXT_HOLDER = new ThreadLocal<>();

    /**
     * 改变当前线程所使用的数据源名称。
     * <p>注意：必须设定正确的数据源名称（你所配置的），错误的数据源名称会导致程序抛错。</p>
     *
     * @param dataSourceName 数据源名称，不能将其设置为null或者空字符串，否则会抛错
     */
    public static void setDataSourceName(String dataSourceName) {
        // 这里需要同步，否则某些情况下切换会有风险
        synchronized (DataSourceContextHolder.class) {
            if (dataSourceName == null || "".equals(dataSourceName.trim())) {
                throw new IllegalArgumentException("Cannot set DataSource name as null or empty string");
            }
            CONTEXT_HOLDER.set(dataSourceName);
        }
    }

    /**
     * 获得当前数据源的名称。
     *
     * @return 数据源名称，当返回的数据源名称为null或者空字符串时代表正在使用默认数据源（你在配置中的第一个数据源）
     */
    public static String getDataSourceName() {
        return CONTEXT_HOLDER.get();
    }

    /**
     * 将当前数据源置为默认值，一般来说默认的数据源是你在配置中所配置的第一个数据源。
     */
    public static void setDataSourceToDefault() {
        CONTEXT_HOLDER.remove();
    }
}
