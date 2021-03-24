package com.newegg.mkpl.radial.mybatis;

import org.apache.commons.logging.LogFactory;
import org.apache.ibatis.logging.Log;

/**
 * MyBatis日志CommonsLog适配器。
 *
 * @author mm92
 * @since 1.2.4 2019-05-23
 */
public class MyBatisCommonsLogAdaptor implements Log {

    private final org.apache.commons.logging.Log LOGGER = LogFactory.getLog(getClass());

    public MyBatisCommonsLogAdaptor(String clazz) {
        // Do nothing
    }

    @Override
    public boolean isDebugEnabled() {
        return LOGGER.isDebugEnabled();
    }

    @Override
    public boolean isTraceEnabled() {
        return LOGGER.isTraceEnabled();
    }

    @Override
    public void error(String s, Throwable e) {
        LOGGER.error(s, e);
    }

    @Override
    public void error(String s) {
        LOGGER.error(s);
    }

    @Override
    public void debug(String s) {
        LOGGER.debug(s);
    }

    @Override
    public void trace(String s) {
        LOGGER.trace(s);
    }

    @Override
    public void warn(String s) {
        LOGGER.warn(s);
    }

}
