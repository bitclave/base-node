package com.bitclave.node.repository.search.state

import com.bitclave.node.repository.models.OfferSearchState

interface OfferSearchStateRepository {

    fun save(state: OfferSearchState): OfferSearchState

    fun save(states: List<OfferSearchState>): List<OfferSearchState>

    fun findByOfferIdAndOwner(offerId: Long, owner: String): OfferSearchState?

    fun findByOfferIdInAndOwnerIn(offerIds: List<Long>, owners: List<String>): List<OfferSearchState>

    fun findByOfferIdInAndOwner(offerIds: List<Long>, owner: String): List<OfferSearchState>

    fun findByOfferId(offerId: Long): List<OfferSearchState>

    fun findByOfferIdIn(offerIds: List<Long>): List<OfferSearchState>

    fun deleteAllByOwner(owner: String): Long

    fun delete(ids: List<Long>): Long
}
