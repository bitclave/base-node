package com.bitclave.node.repository.search.interaction

import com.bitclave.node.repository.models.OfferInteraction

interface OfferInteractionRepository {

    fun save(state: OfferInteraction): OfferInteraction

    fun save(states: List<OfferInteraction>): List<OfferInteraction>

    fun findByOfferIdAndOwner(offerId: Long, owner: String): OfferInteraction?

    fun findByOfferIdInAndOwnerIn(offerIds: List<Long>, owners: List<String>): List<OfferInteraction>

    fun findByOfferIdInAndOwner(offerIds: List<Long>, owner: String): List<OfferInteraction>

    fun findByOfferId(offerId: Long): List<OfferInteraction>

    fun findByOfferIdIn(offerIds: List<Long>): List<OfferInteraction>

    fun deleteAllByOwner(owner: String): Long

    fun delete(ids: List<Long>): Long
}
