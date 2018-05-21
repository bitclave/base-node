package com.bitclave.node.repository.offer

import com.bitclave.node.repository.models.Offer
import com.bitclave.node.services.errors.DataNotSavedException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
@Qualifier("postgres")
class PostgresOfferRepositoryImpl(val repository: OfferCrudRepository) : OfferRepository {

    override fun saveOffer(offer: Offer): Offer {
        return repository.save(offer) ?: throw DataNotSavedException()
    }

    override fun deleteOffer(id: Long, owner: String): Long {
        val count = repository.deleteByIdAndOwner(id, owner)
        if (count > 0) {
            return id
        }

        return 0
    }

    override fun deleteOffers(owner: String): Long {
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

}
