package com.bitclave.node.repository.search.offer

import com.bitclave.node.repository.models.OfferSearch

interface OfferSearchRepository {

    fun saveSearchResult(list: List<OfferSearch>)

    fun saveSearchResult(item: OfferSearch)

    fun findById(id: Long): OfferSearch?

    fun findBySearchRequestId(id: Long): List<OfferSearch>

    fun findByOfferId(id: Long): List<OfferSearch>

    fun findBySearchRequestIdAndOfferId(searchRequestId: Long, offerId: Long): List<OfferSearch>

}
