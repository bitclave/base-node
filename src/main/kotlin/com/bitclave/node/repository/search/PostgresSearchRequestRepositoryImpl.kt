package com.bitclave.node.repository.search

import com.bitclave.node.repository.models.SearchRequest
import com.bitclave.node.repository.search.offer.OfferSearchCrudRepository
import com.bitclave.node.services.errors.DataNotSavedException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.domain.SliceImpl
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.math.BigInteger
import java.util.HashMap
import javax.persistence.EntityManager

@Component
@Qualifier("postgres")
class PostgresSearchRequestRepositoryImpl(
    val repository: SearchRequestCrudRepository,
    val offerSearchRepository: OfferSearchCrudRepository,
    val entityManager: EntityManager
) : SearchRequestRepository {

    override fun save(request: SearchRequest): SearchRequest =
        syncElementCollections(repository.save(request)) ?: throw DataNotSavedException()

    override fun save(request: List<SearchRequest>): List<SearchRequest> = repository.saveAll(request).toList()

    override fun deleteByIdAndOwner(id: Long, owner: String): Long = repository.deleteByIdAndOwner(id, owner)

    override fun deleteByOwner(owner: String): Int {
        repository.deleteTagsByOwner(owner)
        return repository.deleteByOwner(owner)
    }

    override fun deleteByIdIn(ids: List<Long>): Int {
        if (ids.isEmpty()) return 0
        repository.deleteTagsByIdIn(ids)
        return repository.deleteByIdIn(ids)
    }

    override fun findById(id: Long): SearchRequest? {
        return syncElementCollections(repository.findByIdOrNull(id))
    }

    override fun findById(ids: List<Long>): List<SearchRequest> {
        return syncElementCollections(repository.findAllById(ids).toList())
    }

    override fun findByOwner(owner: String): List<SearchRequest> {
        return syncElementCollections(repository.findByOwner(owner))
    }

    override fun findByIdAndOwner(id: Long, owner: String): SearchRequest? {
        return syncElementCollections(repository.findByIdAndOwner(id, owner))
    }

    override fun findAll(): List<SearchRequest> {
        return syncElementCollections(repository.findAll().toList())
    }

    override fun findAll(pageable: Pageable): Page<SearchRequest> {
        return syncElementCollections(repository.findAll(pageable))
    }

    override fun findAllSlice(pageable: Pageable): Slice<SearchRequest> {
        return syncElementCollections(repository.findAllBy(pageable))
    }

    override fun findByOwnerInSlice(owners: List<String>, pageable: Pageable): Slice<SearchRequest> =
        syncElementCollections(repository.findByOwnerIn(owners, pageable))

    override fun getTotalCount(): Long {
        return repository.count()
    }

    override fun getRequestByOwnerAndTag(owner: String, tagKey: String): List<SearchRequest> {
        return syncElementCollections(repository.getRequestByOwnerAndTag(owner, tagKey))
    }

    override fun getSearchRequestWithSameTags(): List<SearchRequest> {
        return syncElementCollections(repository.getSearchRequestWithSameTags())
    }

    private fun deleteRelevantOfferSearches(searchRequestId: Long) {
        val relatedOfferSearches = offerSearchRepository.findBySearchRequestId(searchRequestId)
        offerSearchRepository.deleteAll(relatedOfferSearches)
    }

    private fun syncElementCollections(searchRequest: SearchRequest?): SearchRequest? {
        return if (searchRequest == null) null else syncElementCollections(listOf(searchRequest))[0]
    }

    private fun syncElementCollections(page: Page<SearchRequest>): Page<SearchRequest> {
        val result = syncElementCollections(page.content)
        val pageable = PageRequest.of(page.number, page.size, page.sort)

        return PageImpl(result, pageable, page.totalElements)
    }

    private fun syncElementCollections(slice: Slice<SearchRequest>): Slice<SearchRequest> {
        val result = syncElementCollections(slice.content)
        val pageable = PageRequest.of(slice.number, slice.size, slice.sort)

        return SliceImpl(result, pageable, slice.hasNext())
    }

    private fun syncElementCollections(searchRequests: List<SearchRequest>): List<SearchRequest> {
        val ids = searchRequests.map { it.id }.distinct().joinToString(",")

        if (ids.isEmpty()) {
            return emptyList()
        }

        @Suppress("UNCHECKED_CAST")
        val queryResultTags = entityManager
            .createNativeQuery("SELECT * FROM search_request_tags WHERE search_request_id in ($ids);")
            .resultList as List<Array<Any>>

        val mappedTags = (queryResultTags).groupBy { (it[0] as BigInteger).toLong() }

        return searchRequests.map {
            val tags = HashMap<String, String>()
            mappedTags[it.id]?.forEach { rawTag -> tags[rawTag[2] as String] = rawTag[1] as String }

            return@map it.copy(tags = tags)
        }
    }
}
