package org.maraxma.radial.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

import org.maraxma.radial.mybatis.SwitchableDataSourceOnMapperConfiguration;

/**
 * 开启可切换数据源对于Mapper接口方法的支持。开启后，将可以直接在Mapper接口的抽象方法上标注@UseDataSource进行数据源指定了。
 * <p>注意：在Mapper接口上指定@UseDataSource意味着这个方法被绑定到你所指定的数据源了，在其他地方均无法再切换，并且相互调用会使用各自标注的数据源。
 * 若你不打上此标签（不开启），那么对于Mapper接口方法上的@UseDataSource标签将不会生效且在调用时会在控制台打出警告。</p>
 *
 * @author mm92
 * @since 1.1.7 2019-02-28
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({SwitchableDataSourceOnMapperConfiguration.class})
public @interface EnableSwitchableDataSourceOnMapper {

}
