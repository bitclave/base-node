package com.bitclave.node.repository.price

import com.bitclave.node.repository.entities.Offer
import com.bitclave.node.repository.entities.OfferPrice
import com.bitclave.node.repository.entities.OfferPriceRules
import com.bitclave.node.repository.priceRule.OfferPriceRulesCrudRepository
import com.bitclave.node.services.errors.DataNotSavedException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
@Qualifier("postgres")
class PostgresOfferPriceRepositoryImpl(
    val repository: OfferPriceCrudRepository,
    val rulesRepository: OfferPriceRulesCrudRepository
) : OfferPriceRepository {
    override fun saveAllPrices(prices: List<OfferPrice>): List<OfferPrice> {
        val savedPrices = repository.saveAll(prices)

        val rules = savedPrices.mapIndexed { index, offerPrice ->
            prices[index].rules.map {
                val copiedRule = it.copy()
                copiedRule.offerPrice = offerPrice
                copiedRule
            }
        }.flatten()

        rulesRepository.saveAll(rules)

        return savedPrices.toList()
    }

    override fun deleteAllByOfferIdIn(ids: List<Long>): List<OfferPrice> {
        return repository.deleteAllByOfferIdIn(ids)
    }

    override fun savePrices(offer: Offer, prices: List<OfferPrice>): List<OfferPrice> {
        val copyPrices = prices.map { it.copy() }

        for (price: OfferPrice in copyPrices) {

            price.offer = offer
            val savedPrice = repository.save(price) ?: throw DataNotSavedException()

            for (rule: OfferPriceRules in price.rules) {
                rule.offerPrice = savedPrice
                rulesRepository.save(rule) ?: throw DataNotSavedException()
            }
        }
        return repository.findByOfferId(offer.id)
    }
}
