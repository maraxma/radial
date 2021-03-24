package com.newegg.mkpl.radial.auto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import com.alibaba.druid.pool.DruidDataSource;
import com.newegg.mkpl.radial.aop.SwitchableDataSourceAspect;
import com.newegg.mkpl.radial.datasource.DataSourceFactoryBean;
import com.newegg.mkpl.radial.datasource.SwitchableDataSource;
import com.newegg.mkpl.radial.property.MultiDataSourceProperties;
import com.newegg.mkpl.radial.property.RadialDataSourceProperties;
import com.newegg.mkpl.radial.strategy.DataSourceNamingStrategy;
import com.newegg.mkpl.radial.strategy.DefaultDataSourceNamingStrategy;
import com.zaxxer.hikari.HikariDataSource;

/**
 * 可切换数据源（DataSource）自动装配器，目前支持两种池化数据源实现：HikariCP和DruidCP。
 * <p>注意：如果你的配置中指定了HikariCP或DruidCP而你的classpath下没有这两个实现的相关依赖包，那么程序会报错。</p>
 *
 * @author mm92
 * @since 1.0.0 2018-11-08
 */
@Configuration
@EnableAspectJAutoProxy
@EnableConfigurationProperties(MultiDataSourceProperties.class)
@ConditionalOnClass(DataSource.class)
@Import({SwitchableDataSourceAspect.class, AnnotationPostProcessor.class})
public class SwitchableDataSourceAutoConfiguration implements BeanFactoryAware, EnvironmentAware {

    @SuppressWarnings("unused")
    private BeanFactory beanFactory;

    /* 如下的成员变量可能会用到，暂且保留在这儿吧 */
    @SuppressWarnings("unused")
    private Environment environment;
    @SuppressWarnings("unused")
    private Binder binder;

    private static final Log LOG = LogFactory.getLog(SwitchableDataSourceAutoConfiguration.class);

    @Autowired
    private MultiDataSourceProperties dataSourceProperties;

    @Autowired
    private DataSourceNamingStrategy dataSourceBeanNamingStrategy;

    private static final String HIKARI_DATASOURCE_CLASS_NAME = "com.zaxxer.hikari.HikariDataSource";
    private static final String DRUID_DATASOURCE_CLASS_NAME = "com.alibaba.druid.pool.DruidDataSource";

    @Bean("switchableDataSource")
    @ConditionalOnMissingBean(SwitchableDataSource.class)
    @ConditionalOnClass(HikariDataSource.class)
    public SwitchableDataSource switchableDataSource() {
        if (dataSourceProperties.getDatasources() == null || dataSourceProperties.getDatasources().size() < 1) {
            throw new IllegalArgumentException("Cannot find prop \"radial.datasources\", if you want to configure switchable-datasource, please set it to the spring properties file");
        }
        LOG.info("Processing " + dataSourceProperties.getDatasources().size() + " datasource(s): " + dataSourceProperties.getDatasources().entrySet().stream().collect(ArrayList::new, (l, r) -> l.add(r.getKey()), ArrayList::addAll));
        DataSource firstDataSource = null;
        Map<Object, Object> dataSourceMap = new HashMap<>();
        SwitchableDataSource switchableDataSource = new SwitchableDataSource();
        for (Map.Entry<String, RadialDataSourceProperties> e : dataSourceProperties.getDatasources().entrySet()) {
            String dsName = e.getKey();
            Boolean active = e.getValue().getActive();
            if (active != null && !active) {
                LOG.info("!!! This datasource is ignored because it prop \"active\" is set to false: " + dsName);
            } else {
                RadialDataSourceProperties dsProperties = e.getValue();
                if (dsProperties.getType() == null) {
                    // 在没有设置Type的情况下，从下面的配置名称中判定使用的是那种数据源实现
                    if (dsProperties.getHikari() != null) {
                        // 若下面的节点是"hikari"，那么认为使用的是hikariCP，这是优先考虑的
                        LOG.warn("Datasource \"" + e.getKey() + "\", its type is undefined, but property node \"hikari\" is found, treat it as HikariDataSource");
                        dsProperties.setType(HikariDataSource.class);
                    } else if (dsProperties.getDruid() != null) {
                        LOG.warn("Datasource \"" + e.getKey() + "\", its type is undefined, but property node \"druid\" is found, treat it as DruidDataSource");
                        dsProperties.setType(DruidDataSource.class);
                    } else {
                        LOG.warn("Datasource \"" + e.getKey() + "\", its type is undefined, no property node is found, treat it as HikariDataSource");
                        dsProperties.setType(HikariDataSource.class);
                    }
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
                String beanName = dataSourceBeanNamingStrategy.getName(e.getKey(), dsProperties);
                DataSourceFactoryBean dataSourceFactoryBean = new DataSourceFactoryBean(dsProperties, detailsProp);
                DataSource reslovedDataSource;
                try {
                    reslovedDataSource = dataSourceFactoryBean.getObject();
                } catch (Exception e1) {
                    throw new RuntimeException(e1);
                }
                LOG.info("Registered datasource bean: " + beanName);
                if (firstDataSource == null) {
                    switchableDataSource.setDefaultTargetDataSource(reslovedDataSource);
                    firstDataSource = reslovedDataSource;
                    LOG.info("The default datasource is: " + beanName);
                }
                dataSourceMap.put(beanName, reslovedDataSource); // 默认DataSource也要放进备选集合
            }
        }
        switchableDataSource.setTargetDataSources(dataSourceMap);
        return switchableDataSource;
    }

    @Bean
    @ConditionalOnMissingBean(DataSourceNamingStrategy.class)
    public DataSourceNamingStrategy dataSourceBeanNamingStrategy() {
        LOG.info("No customer's DataSourceBeanNamingStrategy was detected, using DefaultDataSourceBeanNamingStrategy, you can acquire bean with name \"dataSource_XXX\", the holder \"XXX\" is your datasource title in properties file");
        return new DefaultDataSourceNamingStrategy();
    }

    @Bean
    @ConditionalOnBean(DataSourceNamingStrategy.class)
    public Void dataSourceBeanNamingStrategyDetected(DataSourceNamingStrategy dataSourceNamingStrategy) {
        if (!(dataSourceNamingStrategy instanceof DefaultDataSourceNamingStrategy)) {
            LOG.info("Customer's DataSourceBeanNamingStrategy was detected, using " + dataSourceBeanNamingStrategy.getClass().getName());
        }
        return null;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
        binder = Binder.get(environment);
    }
}
