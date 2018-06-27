package com.bitclave.node.repository.data

import com.bitclave.node.repository.models.ClientData
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
interface ClientDataCrudRepository : CrudRepository<ClientData, String> {

    fun deleteByPublicKey(key: String)

}
