package com.bitclave.node.repository.search.offer

import com.bitclave.node.repository.models.OfferResultAction
import com.bitclave.node.repository.models.OfferSearch
import com.bitclave.node.repository.models.SearchRequest
import com.bitclave.node.repository.search.SearchRequestCrudRepository
import com.bitclave.node.repository.search.SearchRequestRepository
import com.bitclave.node.services.errors.BadArgumentException
import com.bitclave.node.services.errors.DataNotSavedException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.util.*

@Component
@Qualifier("postgres")
class PostgresOfferSearchRepositoryImpl(
        val repository: OfferSearchCrudRepository,
        val searchRequestRepository: SearchRequestRepository
        ) : OfferSearchRepository {

    override fun saveSearchResult(list: List<OfferSearch>) {
        repository.save(list)
    }

    override fun saveSearchResult(item: OfferSearch) {
        val searchRequest = searchRequestRepository.findById(item.searchRequestId)
        if(searchRequest != null) {
        var id = item.id
            item.owner = searchRequest.owner
            repository.save(item) ?: throw DataNotSavedException()

            var relatedOfferSearches = findByOwnerAndOfferId(searchRequest.owner, item.offerId)

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
                    repository.save(relatedOfferSearches)
                } else {// if it was an new insert then update it according related OfferSearches if exists
                    //TODO can be implemented more efficient insert
                    for (offerSearch: OfferSearch in relatedOfferSearches) {
                        if (offerSearch.id != item.id) {
                            item.state = offerSearch.state
                            item.lastUpdated = offerSearch.lastUpdated
                            item.events.addAll(offerSearch.events)
                            item.info = offerSearch.info
                            repository.save(item)
                            break
                        }
                    }
                }
            }
        }
        else throw BadArgumentException("search request id not exist")
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

    override fun findByOwner(owner: String): List<OfferSearch> {
        return repository.findByOwner(owner)
    }

    override fun findByOwnerAndOfferId(owner: String, offerId: Long): List<OfferSearch> {
        //val searchRequestList = searchRequestRepository.findByOwner(owner)
        //val searchRequestIDs = searchRequestList.map { it.id }.toSet()

        //val offerSearchList = repository.findByOfferId(offerId)

        //return offerSearchList.filter { searchRequestIDs.contains(it.searchRequestId) }
        return repository.findByOwnerAndOfferId(owner, offerId)
    }

    override fun findAll(pageable: Pageable): Page<OfferSearch> {
        return repository.findAll(pageable)
    }

    override fun cloneOfferSearchOfSearchRequest(sourceSearchRequestId: Long, targetSearchRequest: SearchRequest): List<OfferSearch> {
        val copiedOfferSearchList = repository.findBySearchRequestId(sourceSearchRequestId)
        val existedOfferSearchList = repository.findBySearchRequestId(targetSearchRequest.id)

        var toBeSavedOfferSearched: MutableList<OfferSearch> = mutableListOf()
        for (offerSearch: OfferSearch in copiedOfferSearchList) {
            if(existedOfferSearchList.find { it.offerId == offerSearch.offerId && it.owner == offerSearch.owner } == null) {
                val newOfferSearch = OfferSearch(
                        0,
                        targetSearchRequest.owner,
                        targetSearchRequest.id,
                        offerSearch.offerId,
                        OfferResultAction.NONE,
                        Date().toString(),
                        offerSearch.info,
                        ArrayList()
                )
                toBeSavedOfferSearched.add(newOfferSearch)
            }
        }

        return repository.save(toBeSavedOfferSearched).toList()
    }
}
