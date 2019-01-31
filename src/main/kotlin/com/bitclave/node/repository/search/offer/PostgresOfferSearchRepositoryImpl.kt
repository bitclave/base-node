package com.bitclave.node.repository.search.offer

import com.bitclave.node.repository.models.OfferSearch
import com.bitclave.node.services.errors.DataNotSavedException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
@Qualifier("postgres")
class PostgresOfferSearchRepositoryImpl(
        val repository: OfferSearchCrudRepository
) : OfferSearchRepository {

    override fun saveSearchResult(list: List<OfferSearch>) {
        repository.save(list)
    }

    override fun saveSearchResult(item: OfferSearch) {
        var id = item.id;
        repository.save(item) ?: throw DataNotSavedException()
        if(id > 0) {
            var relatedOfferSearches = repository.findBySearchRequestIdAndOfferId(item.searchRequestId, item.offerId)
            relatedOfferSearches.forEach{offerSearchObj -> offerSearchObj.state = item.state}
            // TODO need to update all OfferSearch state, especially the events
            saveSearchResult(relatedOfferSearches)
        }
    }

    override fun findById(id: Long): OfferSearch? {
        return repository.findOne(id)
    }

    override fun findBySearchRequestId(id: Long): List<OfferSearch> {
        return repository.findBySearchRequestId(id)
    }

    override fun findByOfferId(id: Long): List<OfferSearch> {
        return repository.findByOfferId(id)
    }

    override fun findBySearchRequestIdAndOfferId(searchRequestId: Long, offerId: Long): List<OfferSearch> {
        return repository.findBySearchRequestIdAndOfferId(searchRequestId, offerId)
    }
}
