package com.bitclave.node.repository.search.interaction

import com.bitclave.node.repository.models.OfferInteraction
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
interface OfferInteractionCrudRepository : PagingAndSortingRepository<OfferInteraction, Long> {

    fun findByOfferIdAndOwner(offerId: Long, owner: String): OfferInteraction?

    fun findByOfferIdInAndOwnerIn(offerIds: List<Long>, owners: List<String>): List<OfferInteraction>

    fun findByOfferIdInAndOwner(offerIds: List<Long>, owner: String): List<OfferInteraction>

    fun findByOfferId(offerId: Long): List<OfferInteraction>

    fun findByOfferIdIn(offerIds: List<Long>): List<OfferInteraction>

    fun deleteAllByOwner(owner: String): Long

    fun deleteByIdIn(ids: List<Long>): Long
}
