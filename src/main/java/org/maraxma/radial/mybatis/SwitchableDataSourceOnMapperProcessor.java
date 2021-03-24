package org.maraxma.radial.mybatis;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.maraxma.radial.annotation.UseDataSource;
import org.maraxma.radial.datasource.AbstractSwitchableDataSource;
import org.maraxma.radial.exception.DataSourceNotFoundException;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.ReflectionUtils;

/**
 * 可切换数据源标注在Mapper上的处理器。
 *
 * @author mm92
 * @since 1.1.7 2019-03-01
 */
public class SwitchableDataSourceOnMapperProcessor implements BeanPostProcessor, BeanFactoryAware {

    private BeanFactory beanFactory;
    private static final Log LOG = LogFactory.getLog(SwitchableDataSourceOnMapperProcessor.class);

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        if (!(beanFactory instanceof AutowireCapableBeanFactory)) {
            LOG.warn("The bean factory is not an instance of AutowireCapableBeanFactory, so that the @UseDataSource is no effect on filed, the bean factory is: " + beanFactory.getClass().getName());
            this.beanFactory = null;
        } else {
            this.beanFactory = beanFactory;
        }
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof MapperFactoryBean) {
            @SuppressWarnings("unchecked")
            Class<Object> mapperClass = ((MapperFactoryBean<Object>) bean).getMapperInterface();
            ReflectionUtils.doWithMethods(mapperClass, method -> {
                UseDataSource useDataSourceAnnotation = method.getAnnotation(UseDataSource.class);
                String dataSourceName = useDataSourceAnnotation.value();
                try {
                    DataSource dataSource = ((AutowireCapableBeanFactory) beanFactory).resolveNamedBean(DataSource.class).getBeanInstance();
                    if (dataSource instanceof AbstractSwitchableDataSource) {
                        AbstractSwitchableDataSource abstractSwitchableDataSource = (AbstractSwitchableDataSource) dataSource;
                        if (!abstractSwitchableDataSource.containsDataSource(dataSourceName)) {
                            throw new DataSourceNotFoundException("The datasource is not found: " + dataSourceName + "(at [" + method + "], available datasource(s): " + abstractSwitchableDataSource.getAvailableDataSourceNames() + ")");
                        }
                    } else {
                        LOG.info("Current datasource is not SwitchableDataSource, the @UseDataSource tagged on method is no effect, so that no need to check datasource name on method");
                    }
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }, method -> method.isAnnotationPresent(UseDataSource.class));
        }
        return bean;
    }

    static class MapperFactoryBeanInvocationHandler<T> implements InvocationHandler {

        private final Class<T> targetInterface;

        public MapperFactoryBeanInvocationHandler(Class<T> targetInterface) {
            this.targetInterface = targetInterface;
        }

        @SuppressWarnings("unchecked")
        public T bind() {
            return (T) Proxy.newProxyInstance(targetInterface.getClassLoader(), new Class[] {targetInterface}, this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return method.invoke(this, args);
        }

    }
}
