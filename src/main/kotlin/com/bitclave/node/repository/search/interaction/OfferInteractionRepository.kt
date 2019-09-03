package com.bitclave.node.repository.search.interaction

import com.bitclave.node.repository.entities.OfferAction
import com.bitclave.node.repository.entities.OfferInteraction

interface OfferInteractionRepository {

    fun save(state: OfferInteraction): OfferInteraction

    fun save(states: List<OfferInteraction>): List<OfferInteraction>

    fun findByOwner(owner: String): List<OfferInteraction>

    fun findByOfferIdAndOwner(offerId: Long, owner: String): OfferInteraction?

    fun findByOfferIdInAndOwnerIn(offerIds: List<Long>, owners: List<String>): List<OfferInteraction>

    fun findByOfferIdInAndOwner(offerIds: List<Long>, owner: String): List<OfferInteraction>

    fun findByOfferId(offerId: Long): List<OfferInteraction>

    fun findByOfferIdIn(offerIds: List<Long>): List<OfferInteraction>

    fun findByOwnerAndStateIn(owner: String, states: List<OfferAction>): List<OfferInteraction>

    fun findByOwnerAndOfferIdInAndStateIn(
        owner: String,
        offers: List<Long>,
        states: List<OfferAction>
    ): List<OfferInteraction>

    fun deleteAllByOwner(owner: String): Int

    fun delete(ids: List<Long>): Int

    fun getDanglingOfferInteractions(): List<OfferInteraction>
}
