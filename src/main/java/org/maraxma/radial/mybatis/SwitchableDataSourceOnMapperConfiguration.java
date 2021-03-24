package org.maraxma.radial.mybatis;

import org.maraxma.radial.auto.SwitchableDataSourceOnMapperSymbol;
import org.maraxma.radial.datasource.SwitchableDataSource;
import org.apache.ibatis.binding.MapperProxy;
import org.apache.ibatis.binding.MapperProxyFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;

/**
 * 可切换数据源对于Mapper的支持配置器。
 *
 * @author mm92
 * @since 1.1.7 2019-03-01
 */
@EnableAspectJAutoProxy
@ConditionalOnClass({MapperProxyFactory.class, MapperProxy.class})
@ConditionalOnBean({SwitchableDataSource.class})
@Import({SwitchableDataSourceMyBatisAspect.class, SwitchableDataSourceOnMapperProcessor.class})
public class SwitchableDataSourceOnMapperConfiguration {

    @Bean
    public SwitchableDataSourceOnMapperSymbol switchableDataSourceOnMapperSymbol() {
        return new DefaultSwitchableDataSourceOnMapperSymbol();
    }
}
