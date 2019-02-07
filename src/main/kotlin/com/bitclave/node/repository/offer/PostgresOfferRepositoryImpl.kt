package com.bitclave.node.repository.offer

import com.bitclave.node.repository.models.Offer
import com.bitclave.node.repository.models.OfferResultAction
import com.bitclave.node.repository.search.offer.OfferSearchCrudRepository
import com.bitclave.node.services.errors.DataNotSavedException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
@Qualifier("postgres")
class PostgresOfferRepositoryImpl(
        val repository: OfferCrudRepository,
        val offerSearchRepository: OfferSearchCrudRepository) : OfferRepository {

    override fun saveOffer(offer: Offer): Offer {
        var id = offer.id
        repository.save(offer) ?: throw DataNotSavedException()
        if(id > 0) {
            var relatedOfferSearches = offerSearchRepository.findByOfferId(offer.id)
            relatedOfferSearches = relatedOfferSearches.filterIndexed { ix, element ->
                element.state == OfferResultAction.NONE || element.state == OfferResultAction.REJECT
            }
            offerSearchRepository.delete(relatedOfferSearches)
        }
        return offer
    }

    override fun deleteOffer(id: Long, owner: String): Long {
        val count = repository.deleteByIdAndOwner(id, owner)
        if (count > 0) {
            var relatedOfferSearches = offerSearchRepository.findByOfferId(id)

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

    override fun findById(id: Long): Offer? {
        return repository.findById(id)
    }

    override fun findById(ids: List<Long>): List<Offer> {
        return repository.findAll(ids)
                .asSequence()
                .toList()
    }

    override fun findByIdAndOwner(id: Long, owner: String): Offer? {
        return repository.findByIdAndOwner(id, owner)
    }

    override fun findAll(): List<Offer> {
        return repository.findAll()
                .asSequence()
                .toList()
    }

    override fun findAll(pageable: Pageable): Page<Offer> {
        return repository.findAll(pageable)
    }
}
