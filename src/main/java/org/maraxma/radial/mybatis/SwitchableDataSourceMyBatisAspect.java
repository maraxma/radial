package org.maraxma.radial.mybatis;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.apache.ibatis.binding.MapperProxy;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;

import org.maraxma.radial.annotation.UseDataSource;
import org.maraxma.radial.datasource.DataSourceContextHolder;

/**
 * 可切换数据源Mapper接口切面。
 *
 * @author mm92
 * @since 1.1.7 2019-02-28
 */
@Aspect
public class SwitchableDataSourceMyBatisAspect {

    @Before("execution(@org.maraxma.radial.annotation.UseDataSource * *(..)))")
    public void beforeChanging(JoinPoint point) {
        try {
            Object target = point.getTarget();
            if (target instanceof Proxy) {
                target = getJdkProxyTarget(target);
                if (target instanceof MapperProxy) {
                    Class<?> mapperClass = getMapperClass((MapperProxy<?>) target);
                    String methodName = point.getSignature().getName();
                    Class<?>[] argClass = ((MethodSignature) point.getSignature()).getParameterTypes();
                    // 得到访问的方法对象
                    Method method = mapperClass.getMethod(methodName, argClass);
                    UseDataSource annotation = method.getAnnotation(UseDataSource.class);
                    // 取出注解中的数据源名
                    String dataSourceName = annotation.value();
                    // 切换数据源
                    DataSourceContextHolder.setDataSourceName(dataSourceName);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Object getJdkProxyTarget(Object proxyObject) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        if (!(proxyObject instanceof Proxy)) {
            return proxyObject;
        }
        Field h = proxyObject.getClass().getSuperclass().getDeclaredField("h");
        h.setAccessible(true);
        return h.get(proxyObject);
    }

    public static <T> Class<T> getMapperClass(MapperProxy<T> mapperProxy) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Field mapperInterface = mapperProxy.getClass().getDeclaredField("mapperInterface");
        mapperInterface.setAccessible(true);
        @SuppressWarnings("unchecked")
        Class<T> mapperInterfaceClass = (Class<T>) mapperInterface.get(mapperProxy);
        return mapperInterfaceClass;
    }
}
