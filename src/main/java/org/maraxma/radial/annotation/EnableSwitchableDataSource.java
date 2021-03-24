package org.maraxma.radial.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.maraxma.radial.transaction.SwitchableDataSourceTransactionConfiguration;
import org.springframework.context.annotation.Import;

import org.maraxma.radial.auto.SwitchableDataSourceAutoConfiguration;

/**
 * 开启可切换数据源支持（单实例多数据源模式）。开启后你将可以使用@UseDataSource注解在方法和成员变量上绑定数据源。
 * <p>需要注意的是“多数据源”和“可切换数据源”是冲突的，当两者都启用时，仅“可切换数据源”可用，我们也推荐使用“可切换数据源”。</p>
 * <p>注意：当开启可切换数据源支持的时候会默认开启可切换数据源事务，但是你需要在具体实现方法上使用@Transactional来表名这是一个事务块。</p>
 *
 * @author mm92
 * @since 2018-11-10
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({SwitchableDataSourceAutoConfiguration.class, SwitchableDataSourceTransactionConfiguration.class})
public @interface EnableSwitchableDataSource {

}
