package com.newegg.mkpl.radial.auto;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;

import com.alibaba.druid.pool.DruidDataSource;
import com.newegg.mkpl.radial.datasource.DataSourceFactoryBean;
import com.newegg.mkpl.radial.property.RadialDataSourceProperties;
import com.newegg.mkpl.radial.strategy.DataSourceNamingStrategy;
import com.newegg.mkpl.radial.strategy.DefaultDataSourceNamingStrategy;
import com.zaxxer.hikari.HikariDataSource;

/**
 * 多数据源Bean工厂处理器
 *
 * @author mm92
 * @since 1.0.0 2018-11-14
 */
public class MultiDataSourceBeanFactoryPostProcessor implements BeanFactoryPostProcessor, EnvironmentAware, Ordered {

    private static final Log LOG = LogFactory.getLog(MultiDataSourceBeanFactoryPostProcessor.class);

    private Binder binder;

    private final AtomicBoolean isPrimarySet = new AtomicBoolean(false);

    private static final String PROPERTIES_NODE_NAME = "radial.datasources";

    private static final String HIKARI_DATASOURCE_CLASS_NAME = "com.zaxxer.hikari.HikariDataSource";
    private static final String DRUID_DATASOURCE_CLASS_NAME = "com.alibaba.druid.pool.DruidDataSource";

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }

    @Override
    public void setEnvironment(Environment environment) {
        binder = Binder.get(environment);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        Map<String, RadialDataSourceProperties> dataSourceProperties;
        try {
            dataSourceProperties = binder.bind(PROPERTIES_NODE_NAME, Bindable.mapOf(String.class, RadialDataSourceProperties.class)).get();
        } catch (Exception e) {
            LOG.warn("You have used @EnableMultiDataSource, but we cannot find the properties for it, please config it on '" + PROPERTIES_NODE_NAME + "' node and now will disable the MultiDataSource, you cannot wire any DataSource from ApplicationContext");
            return;
        }
        if (dataSourceProperties == null || dataSourceProperties.size() < 1) {
            LOG.warn("You have used @EnableMultiDataSource, but we cannot find the properties for it, please config it on '" + PROPERTIES_NODE_NAME + "' node and now will disable the MultiDataSource, you cannot wire any DataSource from ApplicationContext");
            return;
        }
        LOG.info("Processing " + dataSourceProperties.size() + " datasource(s): " + dataSourceProperties.entrySet().stream().collect(ArrayList::new, (l, r) -> l.add(r.getKey()), ArrayList::addAll));
        ArrayList<String> managedDataSource = new ArrayList<>();
        DataSourceNamingStrategy dataSourceBeanNamingStrategy = null;
        try {
            dataSourceBeanNamingStrategy = beanFactory.resolveNamedBean(DataSourceNamingStrategy.class).getBeanInstance();
        } catch (Exception e) {
            //NOP
        }
        if (dataSourceBeanNamingStrategy == null) {
            LOG.info("No customer's DataSourceBeanNamingStrategy was detected, using DefaultDataSourceBeanNamingStrategy, you can acquire bean with name \"dataSource_XXX\", the holder \"XXX\" is your datasource title in properties file");
            dataSourceBeanNamingStrategy = new DefaultDataSourceNamingStrategy();
        } else {
            LOG.info("Customer's DataSourceBeanNamingStrategy was detected, using " + dataSourceBeanNamingStrategy.getClass().getName());
        }
        for (Map.Entry<String, RadialDataSourceProperties> e : dataSourceProperties.entrySet()) {
            String dsName = e.getKey();
            Boolean active = e.getValue().getActive();
            if (active != null && !active) {
                LOG.info("!!! This datasource is ignored because it prop \"active\" is set to false: " + dsName);
            } else {
                RadialDataSourceProperties dsProperties = e.getValue();
                if (dsProperties.getType() == null) {
                    // 寻找配置，优先Hikari
                    if (dsProperties.getHikari() != null) {
                        LOG.warn("Datasource \"" + e.getKey() + "\", its type is undefined, but property node \"hikari\" is found, treat it as HikariDataSource");
                    } else if (dsProperties.getDruid() != null) {
                        LOG.warn("Datasource \"" + e.getKey() + "\", its type is undefined, but property node \"druid\" is found, treat it as DruidDataSource");
                        dsProperties.setType(DruidDataSource.class);
                    } else {
                        LOG.warn("Datasource \"" + e.getKey() + "\", its type is undefined, no property node is found, treat it as HikariDataSource");
                    }
                    dsProperties.setType(HikariDataSource.class);
                }
                String className = dsProperties.getType().getName();
                Map<String, Object> detailsProp;
                if (HIKARI_DATASOURCE_CLASS_NAME.equalsIgnoreCase(className)) {
                    detailsProp = dsProperties.getHikari();
                } else if (DRUID_DATASOURCE_CLASS_NAME.equals(className)) {
                    detailsProp = dsProperties.getDruid();
                } else {
                    LOG.warn("Unsupported datasoucre type: " + dsProperties.getType().getName() + ", and this datasource is ignored: " + e.getKey());
                    continue;
                }
                // 注册dataSource
                String beanName = dataSourceBeanNamingStrategy.getName(e.getKey(), dataSourceProperties.get(e.getKey()));
                BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder
                        .genericBeanDefinition(DataSourceFactoryBean.class)
                        .addConstructorArgValue(dsProperties)
                        .addConstructorArgValue(detailsProp)
                        .setScope(BeanDefinition.SCOPE_SINGLETON)
                        .setLazyInit(true)
                        .setDestroyMethodName("destroy");
                BeanDefinition beanDefinition = beanDefinitionBuilder.getBeanDefinition();
                if (!isPrimarySet.get()) {
                    beanDefinition.setPrimary(true);
                    isPrimarySet.set(true);
                }
                ((DefaultListableBeanFactory) beanFactory).registerBeanDefinition(beanName, beanDefinition);
                //BeanDefinitionReaderUtils.registerBeanDefinition(new BeanDefinitionHolder(beanDefinitionBuilder.getBeanDefinition(), beanName), beanFactory);
                managedDataSource.add(beanName);
                LOG.info("Registered datasource bean: " + beanName);
            }
        }
        beanFactory.registerSingleton("managedDataSource", managedDataSource);
    }
}
