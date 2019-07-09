package com.bitclave.node.repository.search.interaction

import com.bitclave.node.repository.models.OfferAction
import com.bitclave.node.repository.models.OfferInteraction
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
@Qualifier("postgres")
class PostgresOfferInteractionRepositoryImpl(
    val repository: OfferInteractionCrudRepository
) : OfferInteractionRepository {

    override fun save(state: OfferInteraction): OfferInteraction = repository.save(state)

    override fun save(states: List<OfferInteraction>): List<OfferInteraction> = repository.save(states).toList()

    override fun findByOwner(owner: String): List<OfferInteraction> = repository.findByOwner(owner)

    override fun findByOfferIdAndOwner(offerId: Long, owner: String): OfferInteraction? =
        repository.findByOfferIdAndOwner(offerId, owner)

    override fun findByOfferIdInAndOwnerIn(offerIds: List<Long>, owners: List<String>): List<OfferInteraction> =
        repository.findByOfferIdInAndOwnerIn(offerIds, owners)

    override fun findByOfferIdInAndOwner(offerIds: List<Long>, owner: String): List<OfferInteraction> =
        repository.findByOfferIdInAndOwner(offerIds, owner)

    override fun findByOfferId(offerId: Long): List<OfferInteraction> = repository.findByOfferId(offerId)

    override fun findByOfferIdIn(offerIds: List<Long>): List<OfferInteraction> = repository.findByOfferIdIn(offerIds)

    override fun findByOwnerAndStateIn(owner: String, states: List<OfferAction>): List<OfferInteraction> =
        repository.findByOwnerAndStateIn(owner, states)

    override fun findByOwnerAndOfferIdInAndStateIn(
        owner: String,
        offers: List<Long>,
        states: List<OfferAction>
    ): List<OfferInteraction> = repository.findByOwnerAndOfferIdInAndStateIn(owner, offers, states)

    override fun deleteAllByOwner(owner: String): Long = repository.deleteAllByOwner(owner)

    override fun delete(ids: List<Long>): Long = repository.deleteByIdIn(ids)
}
