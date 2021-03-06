package com.bitclave.node.repository.search.interaction

import com.bitclave.node.repository.entities.OfferAction
import com.bitclave.node.repository.entities.OfferInteraction
import org.springframework.data.jpa.repository.Modifying
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

    @Modifying
    @Query(
        value = """
            DELETE FROM OfferInteraction oi WHERE oi.owner = ?1
        """
    )
    fun deleteAllByOwner(owner: String): Int

    @Modifying
    @Query(
        value = """
            DELETE FROM OfferInteraction oi WHERE oi.id in ?1
        """
    )
    fun deleteByIdIn(ids: List<Long>): Int

    @Query(
        value = """
            SELECT i FROM OfferInteraction i
            JOIN FETCH i.events
            LEFT JOIN OfferInteraction oi on i.offerId = oi.offerId AND i.owner = oi.owner
            WHERE oi.id is null
        """
    )
    fun getDanglingOfferInteractions(): List<OfferInteraction>

    @Modifying
    @Query(
        value = """
            DELETE FROM offer_interaction_events oie WHERE oie.offer_interaction_id IN ?1
        """,
        nativeQuery = true
    )
    fun deleteEventsByIdIn(ids: List<Long>): Int

    @Modifying
    @Query(
        value = """
            DELETE FROM offer_interaction_events oie WHERE oie.offer_interaction_id IN
            ( SELECT id FROM offer_interaction oi WHERE oi.owner = ?1 )
        """,
        nativeQuery = true
    )
    fun deleteEventsByOwner(owner: String): Int
}
