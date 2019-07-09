package com.bitclave.node.repository.search.interaction

import com.bitclave.node.repository.models.OfferAction
import com.bitclave.node.repository.models.OfferInteraction
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
interface OfferInteractionCrudRepository : PagingAndSortingRepository<OfferInteraction, Long> {

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

    fun deleteAllByOwner(owner: String): Long

    fun deleteByIdIn(ids: List<Long>): Long

    @Query(
        value = """
            SELECT i FROM OfferInteraction i
            JOIN FETCH i.events
            WHERE i.id NOT IN
            ( SELECT oi.id FROM OfferInteraction oi, OfferSearch os
            WHERE os.offerId = oi.offerId AND os.owner = oi.owner)
        """
    )
    fun getDanglingOfferInteractions(): List<OfferInteraction>
}
