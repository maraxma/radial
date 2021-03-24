package org.maraxma.radial.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.maraxma.radial.datasource.DataSourceInfoContextHolder;
import org.maraxma.radial.datasource.DataSourceTracingConfiguration;
import org.springframework.context.annotation.Import;

/**
 * 启用数据源追踪。
 * <p>启用后每次使用数据源会将当前正在使用的数据源记录到{@link DataSourceInfoContextHolder}中。
 * 你可以使用它实时获得当前正在使用的数据源，这对于一些特定的场景非常有用。
 * @author mm92
 * @since 1.3.6 2020-07-29
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({DataSourceTracingConfiguration.class})
public @interface EnableDataSourceTracing {

}
