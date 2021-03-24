package com.newegg.mkpl.radial.auto;

import java.util.ArrayList;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.ReflectionUtils;

import com.newegg.mkpl.radial.annotation.UseDataSource;
import com.newegg.mkpl.radial.datasource.AbstractSwitchableDataSource;
import com.newegg.mkpl.radial.exception.DataSourceNotFoundException;

/**
 * 注解处理器，用于注解注入。
 *
 * @author mm92
 * @since 1.0.0 2018-11-13
 */
public class AnnotationPostProcessor implements BeanPostProcessor, BeanFactoryAware {

    private BeanFactory beanFactory;
    private static final Log LOG = LogFactory.getLog(AnnotationPostProcessor.class);

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
        if (beanFactory != null) {
            checkMethodDataSourcePresent(bean);
            injectDataSourceToField(bean);
        }
        return bean;
    }

    private void injectDataSourceToField(Object bean) {
        ReflectionUtils.doWithFields(bean.getClass(), field -> {
            UseDataSource annotation = field.getAnnotation(UseDataSource.class);
            String dataSourceName = annotation.value();
            DataSource dataSource = ((AutowireCapableBeanFactory) beanFactory).resolveNamedBean(DataSource.class).getBeanInstance();
            if (dataSource instanceof AbstractSwitchableDataSource) {
                AbstractSwitchableDataSource abstractSwitchableDataSource = (AbstractSwitchableDataSource) dataSource;
                if (abstractSwitchableDataSource.containsDataSource(dataSourceName)) {
                    ReflectionUtils.makeAccessible(field);
                    field.set(bean, abstractSwitchableDataSource.getDataSource(dataSourceName));
                } else {
                    if (annotation.required()) {
                        throw new DataSourceNotFoundException("The datasource is not found: " + dataSourceName + "(at [" + field + "], available datasource(s): " + abstractSwitchableDataSource.getAvailableDataSourceNames() + ")");
                    }
                }
            } else if (dataSource instanceof DataSource) {
                if (beanFactory.containsBean(dataSourceName)) {
                    ReflectionUtils.makeAccessible(field);
                    field.set(bean, beanFactory.getBean(dataSourceName));
                } else {
                    if (annotation.required()) {
                        @SuppressWarnings("unchecked")
                        ArrayList<String> managedDataSource = beanFactory.getBean("managedDataSource", ArrayList.class);
                        throw new DataSourceNotFoundException("The datasource is not found: " + dataSourceName + "(at [" + field + "], available datasource(s): " + managedDataSource + ")");
                    }
                }
            }
        }, field -> field.isAnnotationPresent(UseDataSource.class));
    }

    private void checkMethodDataSourcePresent(Object bean) {
        ReflectionUtils.doWithMethods(bean.getClass(), method -> {
            UseDataSource annotation = method.getAnnotation(UseDataSource.class);
            DataSource dataSource = ((AutowireCapableBeanFactory) beanFactory).resolveNamedBean(DataSource.class).getBeanInstance();
            if (dataSource instanceof AbstractSwitchableDataSource) {
                AbstractSwitchableDataSource abstractSwitchableDataSource = (AbstractSwitchableDataSource) dataSource;
                if (!abstractSwitchableDataSource.containsDataSource(annotation.value())) {
                    throw new DataSourceNotFoundException("The datasource is not found: " + annotation.value() + "(at [" + method + "], available datasource(s): " + abstractSwitchableDataSource.getAvailableDataSourceNames() + ")");
                }
            } else {
                LOG.info("Current datasource is not SwitchableDataSource, the @UseDataSource tagged on method is no effect, so that no need to check datasource name on method");
            }
        }, method -> method.isAnnotationPresent(UseDataSource.class));
    }
}
