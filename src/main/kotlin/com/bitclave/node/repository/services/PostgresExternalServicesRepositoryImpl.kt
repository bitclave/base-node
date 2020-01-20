package com.bitclave.node.repository.services

import com.bitclave.node.repository.entities.ExternalService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Qualifier("postgres")
class PostgresExternalServicesRepositoryImpl(
    val repository: ExternalServicesCrudRepository
) : ExternalServicesRepository {

    @Transactional(readOnly = true)
    override fun findById(id: String): ExternalService? = repository.findByIdOrNull(id)

    @Transactional(readOnly = true)
    override fun findAll(): List<ExternalService> = repository.findAll().toList()

    override fun save(entity: ExternalService): ExternalService = repository.save(entity)
}
