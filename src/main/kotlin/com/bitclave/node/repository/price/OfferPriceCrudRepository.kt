package com.bitclave.node.repository.price

import com.bitclave.node.repository.entities.OfferPrice
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import javax.transaction.Transactional

@Repository
@Transactional
interface OfferPriceCrudRepository : CrudRepository<OfferPrice, Long> {

    fun findByOfferId(id: Long): List<OfferPrice>
}
