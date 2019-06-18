package com.bitclave.node.configuration

import net.ttddyy.dsproxy.listener.logging.SLF4JLogLevel
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder
import org.aopalliance.intercept.MethodInterceptor
import org.aopalliance.intercept.MethodInvocation
import org.springframework.aop.framework.ProxyFactory
import org.springframework.beans.BeansException
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.stereotype.Component
import org.springframework.util.ReflectionUtils
import javax.sql.DataSource

@Component
class DatasourceProxyBeanPostProcessor : BeanPostProcessor {

    @Throws(BeansException::class)
    override fun postProcessBeforeInitialization(bean: Any, beanName: String): Any {
        return bean
    }

    @Throws(BeansException::class)
    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
        if (bean is DataSource) {
            val factory = ProxyFactory(bean)
            factory.isProxyTargetClass = true
            factory.addAdvice(ProxyDataSourceInterceptor(bean))
            return factory.proxy
        }
        return bean
    }

    private class ProxyDataSourceInterceptor(dataSource: DataSource) : MethodInterceptor {

        private val dataSource: DataSource
        private val execMap: MutableMap<Int, Long> = HashMap()

        init {
            this.dataSource = ProxyDataSourceBuilder.create(dataSource)
                .countQuery()
                .afterMethod { executionContext ->
                    val startTime = execMap.remove(executionContext.hashCode()) ?: 0
                    System.out.println(
                        "exec info elapsedTime " +
                            "${executionContext.elapsedTime}." +
                            " dt: ${(System.currentTimeMillis() - startTime)}ms"
                    )
                }
                .beforeMethod { executionContext ->
                    execMap[executionContext.hashCode()] = System.currentTimeMillis()
                }
                .logQueryBySlf4j(SLF4JLogLevel.INFO)
                .multiline()
                .build()
        }

        @Throws(Throwable::class)
        override fun invoke(invocation: MethodInvocation): Any {
            val proxyMethod = ReflectionUtils.findMethod(dataSource.javaClass, invocation.method.name)
            return if (proxyMethod != null) {
                proxyMethod.invoke(dataSource, *invocation.arguments)
            } else invocation.proceed()
        }
    }
}
