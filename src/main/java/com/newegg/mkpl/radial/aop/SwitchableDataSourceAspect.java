package com.newegg.mkpl.radial.aop;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;

import com.newegg.mkpl.radial.annotation.UseDataSource;
import com.newegg.mkpl.radial.auto.SwitchableDataSourceOnMapperSymbol;
import com.newegg.mkpl.radial.datasource.DataSourceContextHolder;
import com.newegg.mkpl.radial.datasource.SwitchableDataSource;

/**
 * 可切换数据源切面。
 *
 * @author mm92
 * @since 1.0.0 2018-11-09
 */
@Aspect
@Order(-1)
public class SwitchableDataSourceAspect {

    private static final Log LOG = LogFactory.getLog(SwitchableDataSourceAspect.class);

    @Autowired(required = false)
    private SwitchableDataSourceOnMapperSymbol symbol;
    
    @Autowired(required = false)
    private SwitchableDataSource dataSource;

    @Before("execution(@com.newegg.mkpl.radial.annotation.UseDataSource * *(..)))")
    public void beforeChanging(JoinPoint point) {
        Class<?> clazz = point.getTarget().getClass();
        if (Proxy.isProxyClass(clazz)) {
            try {
                if (symbol == null) {
                    LOG.warn("!!! It sames you are putting @UseDataSource on a abstract method or a interface method and the method's class is proxied by JDK dynamic proxy, if you really want to do, please use @EnableSwitchableDataSourceOnMapper. the original class proxied is: " + getJdkProxyTarget(point.getTarget()));
                }
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            return;
        }
        if (clazz.isInterface()) {
            LOG.warn("It sames you are putting @UseDataSource on an abstract method or a interface method, if you really want to do, please use @EnableSwitchableDataSourceOnMapper");
            return;
        }
        String methodName = point.getSignature().getName();
        Class<?>[] argClass = ((MethodSignature) point.getSignature()).getParameterTypes();
        String dataSourceName;
        try {
            // 得到访问的方法对象
            Method method = clazz.getMethod(methodName, argClass);
            UseDataSource annotation = method.getAnnotation(UseDataSource.class);
            if (annotation == null) {
                throw new IllegalStateException("Unexpected exception, cannot find @UseDataSource on method: " + method + " or on its class");
            } else {
                // 取出注解中的数据源名
                dataSourceName = annotation.value();
            }
            // 切换数据源
            DataSourceContextHolder.setDataSourceName(dataSourceName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    @Before("within(@org.apache.ibatis.annotations.Mapper *)")
    public void beforeMapperMethodExecuting(JoinPoint point) {
    	Class<?> clazz = point.getTarget().getClass();
    	String methodName = point.getSignature().getName();
        Class<?>[] argClass = ((MethodSignature) point.getSignature()).getParameterTypes();
        try {
        	// Bug fix：误报It sames you are executing mybatis mapper method without annotation `@UseDataSource`
        	// 原因，clazz是个代理类，要获得Method必须从原Mapper接口上去获取，这样获取到的Method上才可能标注有UseDataSource
        	Method method = null;
        	if (Proxy.isProxyClass(clazz)) {
        		Class<?>[] interfaces = clazz.getInterfaces();
        		if (interfaces.length > 0) {
        			method = interfaces[0].getMethod(methodName, argClass);
        		}
        	}
        	if (method != null && !method.isAnnotationPresent(UseDataSource.class)) {
        		String currentDataSourceName;
        		if (dataSource != null) {
        			currentDataSourceName = DataSourceContextHolder.getDataSourceName() == null ? dataSource.getDefaultDataSourceName() : DataSourceContextHolder.getDataSourceName();
        		} else {
        			currentDataSourceName = DataSourceContextHolder.getDataSourceName();
        		}
        		LOG.warn("It sames you are executing mybatis mapper method without annotation `@UseDataSource`, the datasource will be used is the previous datasource `" + currentDataSourceName + "`, the method is `" + method + "`");
        	}
        } catch (Exception e) {
			throw new RuntimeException(e);
		}
    }

    public static Object getJdkProxyTarget(Object proxyObject) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        if (!Proxy.isProxyClass(proxyObject.getClass())) {
            return proxyObject;
        }
        Field h = proxyObject.getClass().getSuperclass().getDeclaredField("h");
        h.setAccessible(true);
        return h.get(proxyObject);
    }
}
