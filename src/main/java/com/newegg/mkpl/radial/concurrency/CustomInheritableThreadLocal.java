package com.newegg.mkpl.radial.concurrency;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 定制化的InheritableThreadLocal。
 * <p>为它新增一个静态方法用于构造InheritableThreadLocal，弥补原有InheritableThreadLocal没有快捷初始化方法的缺点。</p>
 *
 * @author mm92
 * @since 1.2.8 2019-09-30
 */
public class CustomInheritableThreadLocal<T> extends InheritableThreadLocal<T> {

    /**
     * 构建一个CustomInheritableThreadLocal。
     *
     * @param <S>        此ThreadLocal里包含的内容的类型
     * @param myValue    新生产的ThreadLocal的值的取值方式
     * @param childValue 子线程中ThreadLocal的值的取值方式
     * @return CustomInheritableThreadLocal对象
     */
    public static <S> CustomInheritableThreadLocal<S> withInitialValue(Supplier<? extends S> myValue, Function<S, ? extends S> childValue) {
        return new SuppliedCustomInheritableThreadLocal<>(myValue, childValue);
    }

    static class SuppliedCustomInheritableThreadLocal<T> extends CustomInheritableThreadLocal<T> {
        private final Supplier<? extends T> myValueSupplier;
        private final Function<T, ? extends T> childValueFunction;

        SuppliedCustomInheritableThreadLocal(Supplier<? extends T> myValueSupplier, Function<T, ? extends T> childValueFunction) {
            this.myValueSupplier = Objects.requireNonNull(myValueSupplier);
            this.childValueFunction = Objects.requireNonNull(childValueFunction);
        }

        @Override
        protected T initialValue() {
            return myValueSupplier.get();
        }

        @Override
        protected T childValue(T parentValue) {
            return childValueFunction.apply(parentValue);
        }
    }
}
