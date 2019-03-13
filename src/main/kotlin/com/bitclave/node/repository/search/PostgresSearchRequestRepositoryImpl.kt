package com.bitclave.node.repository.search

import com.bitclave.node.repository.models.OfferResultAction
import com.bitclave.node.repository.models.OfferSearch
import com.bitclave.node.repository.models.SearchRequest
import com.bitclave.node.repository.search.offer.OfferSearchCrudRepository
import com.bitclave.node.services.errors.BadArgumentException
import com.bitclave.node.services.errors.DataNotSavedException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.util.ArrayList

@Component
@Qualifier("postgres")
class PostgresSearchRequestRepositoryImpl(
    val repository: SearchRequestCrudRepository,
    val offerSearchRepository: OfferSearchCrudRepository
) : SearchRequestRepository {

    override fun saveSearchRequest(request: SearchRequest): SearchRequest {
        val id = request.id
        repository.save(request) ?: throw DataNotSavedException()

        if (id > 0) {
            var relatedOfferSearches = offerSearchRepository.findBySearchRequestId(id)

            relatedOfferSearches = relatedOfferSearches.filter {
                it.state == OfferResultAction.NONE || it.state == OfferResultAction.REJECT
            }

            offerSearchRepository.delete(relatedOfferSearches)
        }

        return request
    }

    override fun deleteSearchRequest(id: Long, owner: String): Long {
        val count = repository.deleteByIdAndOwner(id, owner)
        if (count > 0) {
            var relatedOfferSearches = offerSearchRepository.findBySearchRequestId(id)

            relatedOfferSearches = relatedOfferSearches.filter {
                it.state == OfferResultAction.NONE || it.state == OfferResultAction.REJECT
            }

            offerSearchRepository.delete(relatedOfferSearches)

            return id
        }

        return 0
    }

    override fun deleteSearchRequests(owner: String): Long {
        // TODO delete OfferSearch based on BULK deleted searchRequest
        return repository.deleteByOwner(owner)
    }

    override fun findById(id: Long): SearchRequest? {
        return repository.findOne(id)
    }

    override fun findById(ids: List<Long>): List<SearchRequest> {
        return repository.findAll(ids).toList()
    }

    override fun findByOwner(owner: String): List<SearchRequest> {
        return repository.findByOwner(owner)
    }

    override fun findByOwnerAndTagsIn(owner: String, tagKeys: List<String>): List<SearchRequest> {
        val result = mutableListOf<SearchRequest>()
        tagKeys.forEach {
            result.addAll(repository.getRequestByOwnerAndTag(owner, it))
        }

        return result
    }

    override fun findByIdAndOwner(id: Long, owner: String): SearchRequest? {
        return repository.findByIdAndOwner(id, owner)
    }

    override fun findAll(): List<SearchRequest> {
        return repository.findAll().toList()
    }

    override fun cloneSearchRequestWithOfferSearches(request: SearchRequest): SearchRequest {
        val existingRequest = repository.findOne(request.id)
            ?: throw BadArgumentException("SearchRequest does not exist: ${request.id}")

        val relatedOfferSearches = offerSearchRepository.findBySearchRequestId(existingRequest.id)

        val createSearchRequest = SearchRequest(
            0,
            request.owner,
            request.tags
        )

        repository.save(createSearchRequest)

        val toBeSavedOfferSearched: MutableList<OfferSearch> = mutableListOf()
        for (offerSearch: OfferSearch in relatedOfferSearches) {
            val newOfferSearch = OfferSearch(
                0,
                createSearchRequest.owner,
                createSearchRequest.id,
                offerSearch.offerId,
                OfferResultAction.NONE,
                offerSearch.info,
                ArrayList()
            )
            toBeSavedOfferSearched.add(newOfferSearch)
        }

        offerSearchRepository.save(toBeSavedOfferSearched)

        return createSearchRequest
    }

    override fun findAll(pageable: Pageable): Page<SearchRequest> {
        return repository.findAll(pageable)
    }

    override fun getTotalCount(): Long {
        return repository.count()
    }

    override fun getRequestByOwnerAndTag(owner: String, tagKey: String): List<SearchRequest> {
        return repository.getRequestByOwnerAndTag(owner, tagKey)
    }
}
