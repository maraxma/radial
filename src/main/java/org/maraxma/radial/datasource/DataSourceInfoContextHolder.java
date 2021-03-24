package org.maraxma.radial.datasource;

import javax.sql.DataSource;

import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * 用于记录当前使用的数据源信息。
 * <p>注意，只能保留一个时间点的数据源信息，无法处理类似于一个方法中的所有数据源信息。
 * @author mm92
 * @since v1.3.6 2020-07-29
 */
public class DataSourceInfoContextHolder {
    private static final TransmittableThreadLocal<DataSource> HOLDER = new TransmittableThreadLocal<>();
    
    static void setDataSource(DataSource dataSource) {
        HOLDER.set(dataSource);
    }
    
    public static DataSource getDataSource() {
        return HOLDER.get();
    }
}
