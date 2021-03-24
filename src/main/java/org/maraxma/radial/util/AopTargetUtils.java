package org.maraxma.radial.util;

import java.lang.reflect.Field;

import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.framework.AopProxy;
import org.springframework.aop.support.AopUtils;

/**
 * AOP目标相关的工具。
 * @author mm92
 * @since 1.3.6 2020-10-22
 */
public class AopTargetUtils {

	/**
	 * 获取被Cglib或者JDK代理的目标对象。
	 * 
	 * @param proxy 对象，可能是被代理的
	 * @return 被代理对象，如果传入的对象不是代理对象，或者获取有误，则返回原对象。
	 */
	public static Object getTarget(Object proxy) {

		if (!AopUtils.isAopProxy(proxy)) {
			return proxy;// 不是代理对象
		}

		try {
			if (AopUtils.isJdkDynamicProxy(proxy)) {
				return getJdkDynamicProxyTargetObject(proxy);
			} else { // cglib
				return getCglibProxyTargetObject(proxy);
			}
		} catch (Exception e) {
			return proxy;
		}
	}

	private static Object getCglibProxyTargetObject(Object proxy) throws Exception {
		Field h = proxy.getClass().getDeclaredField("CGLIB$CALLBACK_0");
		h.setAccessible(true);
		Object dynamicAdvisedInterceptor = h.get(proxy);

		Field advised = dynamicAdvisedInterceptor.getClass().getDeclaredField("advised");
		advised.setAccessible(true);

		return ((AdvisedSupport) advised.get(dynamicAdvisedInterceptor)).getTargetSource().getTarget();
	}

	private static Object getJdkDynamicProxyTargetObject(Object proxy) throws Exception {
		Field h = proxy.getClass().getSuperclass().getDeclaredField("h");
		h.setAccessible(true);
		AopProxy aopProxy = (AopProxy) h.get(proxy);

		Field advised = aopProxy.getClass().getDeclaredField("advised");
		advised.setAccessible(true);

		return ((AdvisedSupport) advised.get(aopProxy)).getTargetSource().getTarget();
	}

}