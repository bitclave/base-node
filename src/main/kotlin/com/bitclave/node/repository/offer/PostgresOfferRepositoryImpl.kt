package com.bitclave.node.repository.offer

import com.bitclave.node.repository.models.Offer
import com.bitclave.node.repository.models.OfferResultAction
import com.bitclave.node.repository.models.OfferSearch
import com.bitclave.node.repository.search.offer.OfferSearchCrudRepository
import com.bitclave.node.services.errors.DataNotSavedException
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import kotlin.system.measureTimeMillis

@Component
@Qualifier("postgres")
class PostgresOfferRepositoryImpl(
    val repository: OfferCrudRepository,
    val offerSearchRepository: OfferSearchCrudRepository
) : OfferRepository {

    private val logger = KotlinLogging.logger {}

    override fun saveOffer(offer: Offer): Offer {
        val id = offer.id
        repository.save(offer) ?: throw DataNotSavedException()
        if (id > 0) {
            var relatedOfferSearches: List<OfferSearch> = emptyList()
            val step1 = measureTimeMillis {
                relatedOfferSearches = offerSearchRepository.findByOfferId(offer.id)
            }
            logger.debug { "saveOffer: step 1: ms: $step1, l1: ${relatedOfferSearches.size}" }
            val step2 = measureTimeMillis {
                relatedOfferSearches = relatedOfferSearches.filter { element ->
                    element.state == OfferResultAction.NONE || element.state == OfferResultAction.REJECT
                }
            }
            logger.debug { "saveOffer: step 2: ms: $step2, l1: ${relatedOfferSearches.size}" }
            val step3 = measureTimeMillis {
                offerSearchRepository.delete(relatedOfferSearches)
            }
            logger.debug { "saveOffer: step 3: ms: $step3" }
        }
        return offer
    }

    override fun shallowSaveOffer(offer: Offer): Offer {
        repository.save(offer) ?: throw DataNotSavedException()
        return offer
    }

    override fun deleteOffer(id: Long, owner: String): Long {
        val count = repository.deleteByIdAndOwner(id, owner)
        if (count > 0) {
            val relatedOfferSearches = offerSearchRepository.findByOfferId(id)

            offerSearchRepository.delete(relatedOfferSearches)

            return id
        }

        return 0
    }

    override fun deleteOffers(owner: String): Long {
        // TODO delete OfferSearch based on BULK deleted offers
        return repository.deleteByOwner(owner)
    }

    override fun findByOwner(owner: String): List<Offer> {
        return repository.findByOwner(owner)
    }

    override fun findByOwner(owner: String, pageable: Pageable): Page<Offer> {
        return repository.findByOwner(owner, pageable)
    }

    override fun findById(id: Long): Offer? {
        return repository.findById(id)
    }

    override fun findByIds(ids: List<Long>, pageable: Pageable): Page<Offer> {
        return repository.findAllByIdIn(ids, pageable)
    }

    override fun findByIds(ids: List<Long>): List<Offer> {
        return repository.findAll(ids).toList()
    }

    override fun findByIdAndOwner(id: Long, owner: String): Offer? {
        return repository.findByIdAndOwner(id, owner)
    }

    override fun findAll(): List<Offer> {
        return repository.findAll()
            .toList()
    }

    override fun findAll(pageable: Pageable): Page<Offer> {
        return repository.findAll(pageable)
    }

    override fun getTotalCount(): Long {
        return repository.count()
    }

    override fun getOfferByOwnerAndTag(owner: String, tagKey: String): List<Offer> {
        return repository.getOfferByOwnerAndTag(owner, tagKey)
    }
}
