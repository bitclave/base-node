package com.bitclave.node.repository.search.offer

import com.bitclave.node.repository.models.OfferResultAction
import com.bitclave.node.repository.models.OfferSearch
import com.bitclave.node.repository.models.SearchRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

interface OfferSearchRepository {

    fun deleteAllBySearchRequestId(id: Long): Long

    fun deleteAllByOwner(owner: String): List<Long>

    fun save(list: List<OfferSearch>)

    fun save(item: OfferSearch)

    fun findById(id: Long): OfferSearch?

    fun findById(ids: List<Long>): List<OfferSearch>

    fun findBySearchRequestId(id: Long): List<OfferSearch>

    fun findBySearchRequestId(id: Long, pageable: Pageable): Page<OfferSearch>

    fun findBySearchRequestIds(ids: List<Long>): List<OfferSearch>

    fun findByOfferId(id: Long): List<OfferSearch>

    fun findBySearchRequestIdAndOfferId(searchRequestId: Long, offerId: Long): List<OfferSearch>

    fun findBySearchRequestIdAndOfferIds(searchRequestId: Long, offerIds: List<Long>): List<OfferSearch>

    fun findByOwner(owner: String, sort: Sort?): List<OfferSearch>

    fun findAllByOwnerAndStateIn(owner: String, state: List<OfferResultAction>, sort: Sort?): List<OfferSearch>

    fun findAllByOwnerAndSearchRequestIdIn(owner: String, searchRequestIds: List<Long>, sort: Sort?): List<OfferSearch>

    fun findAllByOwnerAndStateAndSearchRequestIdIn(
        owner: String,
        searchRequestIds: List<Long>,
        state: List<OfferResultAction>,
        sort: Sort?
    ): List<OfferSearch>

    fun findByOwnerAndOfferId(owner: String, offerId: Long): List<OfferSearch>

    fun findByOwnerAndOfferIdIn(owner: String, offerIds: List<Long>): List<OfferSearch>

    fun cloneOfferSearchOfSearchRequest(
        sourceSearchRequestId: Long,
        targetSearchRequest: SearchRequest
    ): List<OfferSearch>

    fun findAll(pageable: Pageable): Page<OfferSearch>

    fun findAll(): List<OfferSearch>

    fun getTotalCount(): Long

    // get offerSearches with the same owner and offerId but different content (status/events)
    fun findAllDiff(): List<OfferSearch>

    fun countBySearchRequestId(id: Long): Long

    fun deleteAllByOfferIdAndStateIn(offerId: Long): Int
}
