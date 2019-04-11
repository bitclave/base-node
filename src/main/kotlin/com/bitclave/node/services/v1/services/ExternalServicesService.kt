package com.bitclave.node.services.v1.services

import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.models.services.ExternalService
import com.bitclave.node.repository.models.services.ServiceCall
import com.bitclave.node.repository.models.services.ServiceResponse
import com.bitclave.node.repository.services.ExternalServicesRepository
import javassist.NotFoundException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
@Qualifier("v1")
class ExternalServicesService(
    private val servicesRepository: RepositoryStrategy<ExternalServicesRepository>,
    private val callStrategy: CallStrategy
) {

    fun externalCall(data: ServiceCall, strategy: RepositoryStrategyType): CompletableFuture<ServiceResponse> {
        return CompletableFuture.supplyAsync {
            val service = servicesRepository
                .changeStrategy(strategy)
                .findById(data.serviceId) ?: throw NotFoundException("Service not found")

            val response = callStrategy.changeStrategy(data.type).execute(service.endpoint, data).get()

            ServiceResponse(response.body, response.statusCodeValue, response.headers)
        }
    }

    fun findAll(strategy: RepositoryStrategyType): CompletableFuture<List<ExternalService>> =
        CompletableFuture.supplyAsync { servicesRepository.changeStrategy(strategy).findAll() }
}
