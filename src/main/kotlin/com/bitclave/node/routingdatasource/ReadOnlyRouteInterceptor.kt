package com.bitclave.node.routingdatasource

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory

import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * ReadOnlyRouteInterceptor routes connections by `@Transaction(readOnly=true|false)`
 */
@Aspect
@Component
@Order(0)
class ReadOnlyRouteInterceptor {

    @Around("@annotation(transactional)")
    @Throws(Throwable::class)
    fun proceed(proceedingJoinPoint: ProceedingJoinPoint, transactional: Transactional): Any? {
        try {
            if (transactional.readOnly) {
                RoutingDataSource.setReplicaRoute()
                logger.info("Routing database call to the read replica")
            }
            return proceedingJoinPoint.proceed()
        } finally {
            RoutingDataSource.clearReplicaRoute()
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(ReadOnlyRouteInterceptor::class.java)
    }
}
