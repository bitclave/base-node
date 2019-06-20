package com.bitclave.node.repository.search.state

import com.bitclave.node.repository.models.OfferSearchState

interface OfferSearchStateRepository {

    fun save(state: OfferSearchState): OfferSearchState

    fun findByOfferIdAndOwner(offerId: Long, owner: String): OfferSearchState?

    fun findByOfferIdInAndOwnerIn(offerIds: List<Long>, owners: List<String>): List<OfferSearchState>

    fun deleteAllByOwner(owner: String)
}
