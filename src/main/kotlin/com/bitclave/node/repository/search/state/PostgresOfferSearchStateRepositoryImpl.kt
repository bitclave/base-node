package com.bitclave.node.repository.search.state

import com.bitclave.node.repository.models.OfferSearchState
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
@Qualifier("postgres")
class PostgresOfferSearchStateRepositoryImpl(
    val repository: OfferSearchStateCrudRepository
) : OfferSearchStateRepository {

    override fun save(state: OfferSearchState): OfferSearchState = repository.save(state)

    override fun save(states: List<OfferSearchState>): List<OfferSearchState> = repository.save(states).toList()

    override fun findByOfferIdAndOwner(offerId: Long, owner: String): OfferSearchState? =
        repository.findByOfferIdAndOwner(offerId, owner)

    override fun findByOfferIdInAndOwnerIn(offerIds: List<Long>, owners: List<String>): List<OfferSearchState> =
        repository.findByOfferIdInAndOwnerIn(offerIds, owners)

    override fun findByOfferIdInAndOwner(offerIds: List<Long>, owner: String): List<OfferSearchState> =
        repository.findByOfferIdInAndOwner(offerIds, owner)

    override fun findByOfferId(offerId: Long): List<OfferSearchState> = repository.findByOfferId(offerId)

    override fun findByOfferIdIn(offerIds: List<Long>): List<OfferSearchState> = repository.findByOfferIdIn(offerIds)

    override fun deleteAllByOwner(owner: String): Long = repository.deleteAllByOwner(owner)

    override fun delete(ids: List<Long>): Long = repository.deleteByIdIn(ids)
}
