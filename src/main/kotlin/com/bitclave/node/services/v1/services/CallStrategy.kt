package com.bitclave.node.services.v1.services

import com.bitclave.node.models.services.ServiceCall
import com.bitclave.node.models.services.ServiceCallType
import org.springframework.http.ResponseEntity
import java.util.concurrent.CompletableFuture

interface CallStrategy : CallStrategyExecutor<ServiceCall> {
    fun changeStrategy(type: ServiceCallType): CallStrategyExecutor<ServiceCall>
}

interface CallStrategyExecutor<out T : ServiceCall> {

    fun execute(endPointUrl: String, request: @UnsafeVariance T): CompletableFuture<ResponseEntity<*>?>
}
