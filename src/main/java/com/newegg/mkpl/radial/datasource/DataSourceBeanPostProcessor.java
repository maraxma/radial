package com.newegg.mkpl.radial.datasource;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.springframework.aop.framework.AbstractAdvisingBeanPostProcessor;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.NameMatchMethodPointcut;
import org.springframework.beans.BeansException;

/**
 * DataSource Bean处理器。
 * <p>这个处理器主要代理所有通过radial注册到IoC和{@link SwitchableDataSource}中的DataSource对象。</p>
 * @author mm92
 * @since 1.3.6 2020-07-29
 */
public class DataSourceBeanPostProcessor extends AbstractAdvisingBeanPostProcessor {

    private static final String METHOD_NAME = "getConnection";

    private static final NameMatchMethodPointcut NAMED_METHOD_POINTCUT = new NameMatchMethodPointcut();

    @PostConstruct
    public void init() {
        NAMED_METHOD_POINTCUT.addMethodName(METHOD_NAME);
        this.advisor = new DefaultPointcutAdvisor(NAMED_METHOD_POINTCUT, new DataSourceMethodInterceptor());
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof AbstractSwitchableDataSource) {
            // 如果是可切换数据源，那么不代理可切换数据源本身，而是将其管理的所有数据源代理
            AbstractSwitchableDataSource abstractSwitchableDataSource = (AbstractSwitchableDataSource) bean;
            Map<String, DataSource> existingDataSources = abstractSwitchableDataSource.getAllDataSources();
            Map<Object, Object> proxiedDataSources = existingDataSources.entrySet().stream().collect(HashMap::new, (l, r) -> l.put(r.getKey(), applyProxy(r.getValue(), "")), HashMap::putAll);
            abstractSwitchableDataSource.setTargetDataSources(proxiedDataSources);
            // 重新初始化
            abstractSwitchableDataSource.afterPropertiesSet();
        } else if (bean instanceof DataSource) {
            // 如果是普通数据源，直接代理
            return applyProxy(bean, beanName);
        }
        return bean;
    }

    private Object applyProxy(Object bean, String beanName) {
        if (bean instanceof AopInfrastructureBean) {
            // Ignore AOP infrastructure such as scoped proxies.
            return bean;
        }

        if (bean instanceof Advised) {
            Advised advised = (Advised) bean;
            if (!advised.isFrozen() && isEligible(AopUtils.getTargetClass(bean))) {
                // Add our local Advisor to the existing proxy's Advisor chain...
                if (this.beforeExistingAdvisors) {
                    advised.addAdvisor(0, this.advisor);
                } else {
                    advised.addAdvisor(this.advisor);
                }
                return bean;
            }
        }

        if (isEligible(bean, beanName)) {
            ProxyFactory proxyFactory = prepareProxyFactory(bean, beanName);
            proxyFactory.setProxyTargetClass(true);
            if (!proxyFactory.isProxyTargetClass()) {
                evaluateProxyInterfaces(bean.getClass(), proxyFactory);
            }
            proxyFactory.addAdvisor(this.advisor);
            customizeProxyFactory(proxyFactory);
            return proxyFactory.getProxy(getProxyClassLoader());
        }

        // No proxy needed.
        return bean;
    }
}
