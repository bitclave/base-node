package com.bitclave.node.repository.search.interaction

import com.bitclave.node.repository.models.OfferAction
import com.bitclave.node.repository.models.OfferInteraction
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.math.BigInteger
import javax.persistence.EntityManager

@Component
@Qualifier("postgres")
class PostgresOfferInteractionRepositoryImpl(
    val repository: OfferInteractionCrudRepository,
    val entityManager: EntityManager
) : OfferInteractionRepository {

    override fun save(state: OfferInteraction): OfferInteraction = repository.save(state)

    override fun save(states: List<OfferInteraction>): List<OfferInteraction> =
        syncElementCollections(repository.save(states).toList())

    override fun findByOwner(owner: String): List<OfferInteraction> =
        syncElementCollections(repository.findByOwner(owner))

    override fun findByOfferIdAndOwner(offerId: Long, owner: String): OfferInteraction? =
        syncElementCollections(repository.findByOfferIdAndOwner(offerId, owner))

    override fun findByOfferIdInAndOwnerIn(offerIds: List<Long>, owners: List<String>): List<OfferInteraction> =
        syncElementCollections(repository.findByOfferIdInAndOwnerIn(offerIds, owners))

    override fun findByOfferIdInAndOwner(offerIds: List<Long>, owner: String): List<OfferInteraction> =
        syncElementCollections(repository.findByOfferIdInAndOwner(offerIds, owner))

    override fun findByOfferId(offerId: Long): List<OfferInteraction> =
        syncElementCollections(repository.findByOfferId(offerId))

    override fun findByOfferIdIn(offerIds: List<Long>): List<OfferInteraction> =
        syncElementCollections(repository.findByOfferIdIn(offerIds))

    override fun findByOwnerAndStateIn(owner: String, states: List<OfferAction>): List<OfferInteraction> =
        syncElementCollections(repository.findByOwnerAndStateIn(owner, states))

    override fun findByOwnerAndOfferIdInAndStateIn(
        owner: String,
        offers: List<Long>,
        states: List<OfferAction>
    ): List<OfferInteraction> =
        syncElementCollections(repository.findByOwnerAndOfferIdInAndStateIn(owner, offers, states))

    override fun deleteAllByOwner(owner: String): Long = repository.deleteAllByOwner(owner)

    override fun delete(ids: List<Long>): Long = repository.deleteByIdIn(ids)

    private fun syncElementCollections(interaction: OfferInteraction?): OfferInteraction? {
        return if (interaction == null) null else syncElementCollections(listOf(interaction))[0]
    }

    private fun syncElementCollections(page: Page<OfferInteraction>): Page<OfferInteraction> {
        val result = syncElementCollections(page.content)
        val pageable = PageRequest(page.number, page.size, page.sort)

        return PageImpl(result, pageable, result.size.toLong())
    }

    private fun syncElementCollections(interactions: List<OfferInteraction>): List<OfferInteraction> {
        val ids = interactions.map { it.id }.distinct().joinToString(",")

        if (ids.isEmpty()) {
            return emptyList()
        }

        @Suppress("UNCHECKED_CAST")
        val queryResultEvents = entityManager
            .createNativeQuery("SELECT * FROM offer_interaction_events WHERE offer_interaction_id in ($ids);")
            .resultList as List<Array<Any>>

        val mappedEvents = (queryResultEvents).groupBy { (it[0] as BigInteger).toLong() }

        return interactions.map {
            val events = mappedEvents[it.id]?.map { event -> event[1] as String } ?: emptyList()

            return@map it.copy(events = events)
        }
    }
}
