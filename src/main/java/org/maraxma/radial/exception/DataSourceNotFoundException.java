package org.maraxma.radial.exception;

/**
 * 代表一个指定的数据源并未找到。
 *
 * @author mm92
 * @since 2018-11-10
 */
public class DataSourceNotFoundException extends RuntimeException {
    private static final long serialVersionUID = -4771326853068009395L;

    public DataSourceNotFoundException(String msg) {
        super(msg);
    }
}
