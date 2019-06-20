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

    override fun findByOfferIdAndOwner(offerId: Long, owner: String): OfferSearchState? =
        repository.findByOfferIdAndOwner(offerId, owner)

    override fun findByOfferIdInAndOwnerIn(offerIds: List<Long>, owners: List<String>): List<OfferSearchState> =
        repository.findByOfferIdInAndOwnerIn(offerIds, owners)

    override fun deleteAllByOwner(owner: String) = repository.deleteAllByOwner(owner)
}
