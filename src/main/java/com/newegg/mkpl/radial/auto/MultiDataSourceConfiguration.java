package com.newegg.mkpl.radial.auto;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.newegg.mkpl.radial.datasource.SwitchableDataSource;

/**
 * 多数据源配置类
 *
 * @author mm92
 * @since 1.0.0 2018-11-14
 */
@Import({AnnotationPostProcessor.class})
public class MultiDataSourceConfiguration {

    @Bean
    @ConditionalOnMissingBean({SwitchableDataSource.class})
    @ConditionalOnClass({DataSource.class})
    public static MultiDataSourceBeanFactoryPostProcessor beanFactoryPostProcessor() {
        return new MultiDataSourceBeanFactoryPostProcessor();
    }

}
