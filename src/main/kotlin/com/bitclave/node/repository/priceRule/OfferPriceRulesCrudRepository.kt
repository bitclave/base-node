package com.bitclave.node.repository.priceRule

import com.bitclave.node.repository.entities.OfferPriceRules
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
interface OfferPriceRulesCrudRepository : CrudRepository<OfferPriceRules, Long> {

    @Transactional(readOnly = true)
    fun findByOfferPriceId(offerPriceId: Long): List<OfferPriceRules>
}
