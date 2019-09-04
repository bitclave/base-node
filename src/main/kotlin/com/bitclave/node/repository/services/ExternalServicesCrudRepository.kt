package com.bitclave.node.repository.services

import com.bitclave.node.repository.entities.ExternalService
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
interface ExternalServicesCrudRepository : CrudRepository<ExternalService, String>
