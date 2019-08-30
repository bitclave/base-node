package com.bitclave.node.repository.services

import com.bitclave.node.repository.entities.ExternalService

interface ExternalServicesRepository {

    fun findById(id: String): ExternalService?

    fun findAll(): List<ExternalService>

    fun save(entity: ExternalService): ExternalService
}
