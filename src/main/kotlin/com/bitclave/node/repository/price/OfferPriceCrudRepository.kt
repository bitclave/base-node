package com.bitclave.node.repository.price

import com.bitclave.node.repository.entities.OfferPrice
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
interface OfferPriceCrudRepository : CrudRepository<OfferPrice, Long> {

    @Transactional(readOnly = true)
    fun findByOfferId(id: Long): List<OfferPrice>

    fun deleteAllByOfferIdIn(ids: List<Long>): List<OfferPrice>
}
