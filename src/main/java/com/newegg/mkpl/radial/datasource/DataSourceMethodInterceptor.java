package com.newegg.mkpl.radial.datasource;

import javax.sql.DataSource;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * DataSource方法拦截器。
 * <p>此类主要拦截将拦截的DataSource存放到上下文保持器中。
 * @author mm92
 * @since 1.3.6 2020-07-29
 */
public class DataSourceMethodInterceptor implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Object target = invocation.getThis();
        if (target instanceof DataSource) {
            DataSourceInfoContextHolder.setDataSource((DataSource) target);
        }
        return invocation.proceed();
    }

}
