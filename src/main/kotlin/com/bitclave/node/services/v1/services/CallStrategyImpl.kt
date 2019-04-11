package com.bitclave.node.services.v1.services

import com.bitclave.node.repository.models.services.ServiceCall
import com.bitclave.node.repository.models.services.ServiceCallType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.util.concurrent.CompletableFuture

@Component
class CallStrategyImpl(
    private val restTemplate: RestTemplate
) : CallStrategy {
    private val availableStrategies: Map<ServiceCallType, CallStrategyExecutor<ServiceCall>> =
        hashMapOf(ServiceCallType.HTTP to CallStrategyExecutorHttp(restTemplate))

    private lateinit var strategy: CallStrategyExecutor<ServiceCall>

    init {
        changeStrategy(ServiceCallType.HTTP)
    }

    final override fun changeStrategy(type: ServiceCallType): CallStrategyExecutor<ServiceCall> {
        if (!availableStrategies.containsKey(type)) {
            throw RuntimeException("not supported strategy $type")
        }
        strategy = availableStrategies.getValue(type)

        return strategy
    }

    override fun execute(endPointUrl: String, request: ServiceCall): CompletableFuture<ResponseEntity<*>> =
        strategy.execute(endPointUrl, request)
}
