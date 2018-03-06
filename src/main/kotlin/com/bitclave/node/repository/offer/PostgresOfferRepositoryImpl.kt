package com.bitclave.node.repository.offer

import com.bitclave.node.repository.models.Offer
import com.bitclave.node.services.errors.DataNotSaved
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
@Qualifier("postgres")
class PostgresOfferRepositoryImpl(val repository: OfferCrudRepository) : OfferRepository {

    override fun saveOffer(offer: Offer): Offer {
        return repository.save(offer) ?: throw DataNotSaved()
    }

    override fun findByOwner(owner: String): List<Offer> {
        return repository.findByOwner(owner)
    }

    override fun findByIdAndOwner(id: Long, owner: String): Offer? {
        return repository.findByIdAndOwner(id, owner)
    }

}
