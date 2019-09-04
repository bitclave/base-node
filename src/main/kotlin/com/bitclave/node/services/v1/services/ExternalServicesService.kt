package com.bitclave.node.services.v1.services

import com.bitclave.node.models.services.ServiceCall
import com.bitclave.node.models.services.ServiceResponse
import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.entities.ExternalService
import com.bitclave.node.repository.services.ExternalServicesRepository
import com.bitclave.node.services.errors.BadArgumentException
import com.bitclave.node.utils.supplyAsyncEx
import javassist.NotFoundException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier

@Service
@Qualifier("v1")
class ExternalServicesService(
    private val servicesRepository: RepositoryStrategy<ExternalServicesRepository>,
    private val callStrategy: CallStrategy
) {

    fun externalCall(data: ServiceCall, strategy: RepositoryStrategyType): CompletableFuture<ServiceResponse> {
        return supplyAsyncEx(Supplier {
            val service = servicesRepository
                .changeStrategy(strategy)
                .findById(data.serviceId) ?: throw NotFoundException("Service not found")

            val response = callStrategy.changeStrategy(data.type).execute(service.endpoint, data).get()
                ?: throw BadArgumentException("wrong request")

            ServiceResponse(response.body, response.statusCodeValue, response.headers)
        })
    }

    fun findAll(strategy: RepositoryStrategyType): CompletableFuture<List<ExternalService>> =
        supplyAsyncEx(Supplier { servicesRepository.changeStrategy(strategy).findAll() })
}
