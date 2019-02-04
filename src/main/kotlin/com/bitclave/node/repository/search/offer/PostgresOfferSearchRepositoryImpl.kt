package com.bitclave.node.repository.search.offer

import com.bitclave.node.repository.models.OfferSearch
import com.bitclave.node.repository.search.SearchRequestCrudRepository
import com.bitclave.node.services.errors.DataNotSavedException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
@Qualifier("postgres")
class PostgresOfferSearchRepositoryImpl(
        val repository: OfferSearchCrudRepository,
        val searchRequestRepository: SearchRequestCrudRepository
        ) : OfferSearchRepository {

    override fun saveSearchResult(list: List<OfferSearch>) {
        repository.save(list)
    }

    override fun saveSearchResult(item: OfferSearch) {
        var id = item.id;
        repository.save(item) ?: throw DataNotSavedException()

        val searchRequest = searchRequestRepository.findById(item.searchRequestId)
        if(searchRequest.isNotEmpty()) {
            var relatedOfferSearches = findByOwnerAndOfferId(searchRequest[0].owner, item.offerId)

            if(relatedOfferSearches.size > 1) {
                if (id > 0) {// if it was an update then update all related OfferSearches
                    for (offerSearch: OfferSearch in relatedOfferSearches) {
                        if (offerSearch.id != item.id) {
                            offerSearch.state = item.state
                            offerSearch.lastUpdated = item.lastUpdated
                            offerSearch.events = item.events
                            offerSearch.info = item.info
                        }
                    }

                    saveSearchResult(relatedOfferSearches)
                } else {// if it was an new insert then update it according related OfferSearches if exists
                    //TODO can be implemented more efficient insert
                    for (offerSearch: OfferSearch in relatedOfferSearches) {
                        if (offerSearch.id != item.id) {
                            item.state = offerSearch.state
                            item.lastUpdated = offerSearch.lastUpdated
                            item.events = offerSearch.events
                            item.info = offerSearch.info
                            repository.save(item)
                            break
                        }
                    }
                }
            }
        }
    }

    override fun findById(id: Long): OfferSearch? {
        return repository.findOne(id)
    }

    override fun findBySearchRequestId(id: Long): List<OfferSearch> {
        return repository.findBySearchRequestId(id)
    }

    override fun findBySearchRequestIds(ids: List<Long>): List<OfferSearch> {
        return repository.findBySearchRequestIdIn(ids)
    }

    override fun findByOfferId(id: Long): List<OfferSearch> {
        return repository.findByOfferId(id)
    }

    override fun findBySearchRequestIdAndOfferId(searchRequestId: Long, offerId: Long): List<OfferSearch> {
        return repository.findBySearchRequestIdAndOfferId(searchRequestId, offerId)
    }

    //TODO Later OfferSearch model can be changed in order to cover this need
    override fun findByOwnerAndOfferId(owner: String, offerId: Long): List<OfferSearch> {
        val searchRequestList = searchRequestRepository.findByOwner(owner)
        val searchRequestIDs = searchRequestList.map { it.id }.toSet()

        val offerSearchList = repository.findByOfferId(offerId)

        return offerSearchList.filter { searchRequestIDs.contains(it.searchRequestId) }
    }

    override fun findAll(pageable: Pageable): Page<OfferSearch> {
        return repository.findAll(pageable)
    }
}
