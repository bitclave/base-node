package com.bitclave.node.repository.price

import com.bitclave.node.repository.entities.Offer
import com.bitclave.node.repository.entities.OfferPrice
import com.bitclave.node.repository.entities.OfferPriceRules
import com.bitclave.node.repository.priceRule.OfferPriceRulesCrudRepository
import com.bitclave.node.services.errors.DataNotSavedException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.lang.RuntimeException
import javax.persistence.EntityManager

@Component
@Qualifier("postgres")
class PostgresOfferPriceRepositoryImpl(
    val repository: OfferPriceCrudRepository,
    val rulesRepository: OfferPriceRulesCrudRepository,
    val entityManager: EntityManager
) : OfferPriceRepository {

    @Transactional
    override fun saveAllPrices(prices: List<OfferPrice>, offerIds: List<Long>): List<OfferPrice> {

        val offerIdsIn = offerIds.joinToString(", ")
//        val priceIds = prices.map { it.id }.joinToString(", ")

//        val cleanUpQuery = "DELETE FROM offer_price_rules r\n" +
//            "WHERE r.offer_price_id IN (\n" +
//            "    select id from offer_price p\n" +
//            "    where p.offer_id in ($offerIdsIn) AND p.id NOT IN  ($priceIds)\n" +
//            ");" +
//            "DELETE FROM offer_price p WHERE p.offer_id in ($offerIdsIn) AND p.id NOT IN ($priceIds)"
//
//        entityManager.createNativeQuery(cleanUpQuery).executeUpdate()
        val checkQuery = "SELECT id FROM offer_price p WHERE p.offer_id in ($offerIdsIn)"
        @Suppress("UNCHECKED_CAST")
        val formerIds = entityManager.createNativeQuery(checkQuery).resultList as List<Long>

        if (formerIds.size > prices.size) {
            throw RuntimeException("attempt to reduce prices in bulk of offer")
        }


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
