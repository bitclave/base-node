package com.bitclave.node.repository.priceRule

import com.bitclave.node.repository.models.OfferPriceRules
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import javax.transaction.Transactional

@Repository
@Transactional
interface OfferPriceRulesCrudRepository : CrudRepository<OfferPriceRules, Long> {

    fun findByOfferPriceId(offerPriceId: Long): List<OfferPriceRules>
}
