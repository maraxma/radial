package org.maraxma.radial.annotation;

import org.maraxma.radial.datasource.DataSourceContextHolder;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @UseDataSource}注解可以帮助你在方法上切换该方法使用的数据源。<span style="color:red">此注解作为成员变量标注时只能标注在DataSource及其子类上，标注在其他地方将不产生任何作用。</span>
 * <p style="color:red;font-weight:bold">这个注解非常重要，它代表了多数据源的使用方式，请仔细参见如下的说明：</p>
 * <ul>
 * <li>当使用可切换数据源（{@code @EnableSwitchableDataSource}）时：该注解能用在方法和成员变量上。使用{@code @UseDataSource("XX")}来指定你的数据源。
 * 我们推荐在Service的实现类的方法上标注此注解以指定在执行这个方法时所应该使用的数据源。
 * 请不要尝试在抽象方法、接口类的方法上标注此接口，它在这些地方不起作用。
 * 若一个方法调用了另一个方法，发起调用的方法具有{@code @UseDataSource}注解，即便被调用的方法上注明有{@code @UseDataSource}，那么在本次调用中仍然会延续发起调用方法上的DataSource（仅调用方法上的{@code @UseDataSource}会生效）。
 * 若需要在代码中根据情况灵活切换DataSource，请使用{@link DataSourceContextHolder}，使用其setDataSourceName()方法即可。</li>
 * <p>
 * <pre>
 * 仔细观察如下的两个方法，它们都标注有{@code @UseDataSource}，其中funA调用了funB:
 *
 * @UseDataSource("dataSource_DB1")
 * public String funA() {
 *    String r = DBMapper.findName();
 *    String s = funB();
 *    return r + s;
 * }
 *
 * @UseDataSource("dataSource_DB2")
 * public String funB() {
 *     return DBMapper.findTitle();
 * }
 *
 * 那么在外部执行funA的时候，使用的数据源到底是DB1还是DB2，还是都有使用？
 * 答：只使用了DB1这个数据源。意即，funB标注的{@code @UseDataSource}仅在它在外部被单独调用时才生效。若需要funA使用DB1，而funB使用DB2完成查询，那么应该在funA的函数体中使用{@code DataSourceContextHolder.setDataSourceName("dataSource_DB2")}。
 *
 * public String funA() {
 *    String r = DBMapper.findName();
 *    // 在使用funB查询时手动切换数据源
 *    DataSourceContextHolder.setDataSourceName("dataSource_DB2")
 *    // 然后再调用funB
 *    String s = funB();
 *    return r + s;
 * }
 * </pre>
 * </p>
 * 当你使用可切换数据源时，在有数据操作的方法上而没有使用@UseDataSource时，那么会使用你在配置文件中指定的第一个数据源。两个标有{@code @UseDataSource}的方法互相调用将使用待用发起方的{@code @UseDataSource}。在Controller上调用两个标有不同{@code @UseDataSource}的方法将使用各自的DataSource。
 * <li>当使用多数据源（{@code @EnableMultiDataSource}）时：该注解只能用在成员变量上，在这种情况下它等同于{@code @Resource(name = "XX")}。在成员变量上使用{@code @UseDataSource("XX")}来指定你的数据源，在方法上使用它将没有任何效果。</li>
 * <li>在1.1.7以后，{@code @UseDataSource}可以标注在Mapper接口的抽象方法上了。但是前提是你在入口类上标注了{@link EnableSwitchableDataSourceOnMapper}。关于这种用法的注意事项也请参见{@link EnableSwitchableDataSourceOnMapper}这个类。</li>
 * </ul>
 *
 * @author mm92
 * @see EnableSwitchableDataSourceOnMapper
 * @since 1.0.0 2018-11-10
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface UseDataSource {
    /**
     * 数据源名称，指定你所需要使用的数据源。
     */
    String value();

    /**
     * 是否是必要的，默认为“true”。
     * <p>如果是必要的，那么程序将会在启动时检查是否存在此数据源，这将有助于我们发现错误；
     * 否则程序将会不检查，错误到运行时才会抛出，这有助于测试或者调试。
     * </p>
     */
    boolean required() default true;

}
