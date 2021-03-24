package org.maraxma.radial.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

import org.maraxma.radial.auto.MultiDataSourceConfiguration;

/**
 * 启用多数据源。启用后你的Spring容器里将会出现多个DataSource，你可以任意使用@Autowired选用。
 * <p>需要注意的是“多数据源”和“可切换数据源”是冲突的，当两者都启用时，仅“可切换数据源”可用，我们也推荐使用“可切换数据源”。</p>
 *
 * @author mm92
 * @since 1.0.0 2018-11-14
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({MultiDataSourceConfiguration.class})
public @interface EnableMultiDataSource {

}
