package com.bitclave.node.repository.services

import com.bitclave.node.repository.models.services.ExternalService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
@Qualifier("postgres")
class PostgresExternalServicesRepositoryImpl(
    val repository: ExternalServicesCrudRepository
) : ExternalServicesRepository {

    override fun findById(id: String): ExternalService? = repository.findByIdOrNull(id)

    override fun findAll(): List<ExternalService> = repository.findAll().toList()

    override fun save(entity: ExternalService): ExternalService = repository.save(entity)
}
