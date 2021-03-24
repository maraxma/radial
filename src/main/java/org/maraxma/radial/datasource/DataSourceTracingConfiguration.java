package org.maraxma.radial.datasource;

import org.springframework.context.annotation.Bean;

/**
 * 数据源Bean预处理器。
 * <p>
 * @author mm92
 * @since v1.3.6 2020-07-29
 */
public class DataSourceTracingConfiguration {
    
    @Bean
    public static DataSourceBeanPostProcessor dataSourceBeanPostProcessor() {
        DataSourceBeanPostProcessor beanPostProcessor = new DataSourceBeanPostProcessor();
        beanPostProcessor.setOrder(DataSourceBeanPostProcessor.LOWEST_PRECEDENCE);
        return beanPostProcessor;
    }
}
