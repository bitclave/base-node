package com.bitclave.node.repository.offer

import com.bitclave.node.repository.models.Offer
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
interface OfferCrudRepository : CrudRepository<Offer, Long> {

    fun findByOwner(owner: String): List<Offer>

    fun deleteByIdAndOwner(id: Long, owner: String): Long

    fun deleteByOwner(owner: String): Long

    fun findByIdAndOwner(id: Long, owner: String): Offer?

    fun findById(id: Long): Offer?

}
