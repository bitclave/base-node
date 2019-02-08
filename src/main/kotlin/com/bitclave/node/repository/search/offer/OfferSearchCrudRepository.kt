package com.bitclave.node.repository.search.offer

import com.bitclave.node.repository.models.OfferSearch
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
interface OfferSearchCrudRepository : PagingAndSortingRepository<OfferSearch, Long> {

    fun findBySearchRequestId(id: Long): List<OfferSearch>

    fun findByOfferId(id: Long): List<OfferSearch>

    fun findBySearchRequestIdAndOfferId(searchRequestId: Long, offerId: Long): List<OfferSearch>

    fun findBySearchRequestIdIn(searchRequestIds: List<Long>): List<OfferSearch>

    fun findByOwner(owner: String): List<OfferSearch>

    fun findByOwnerAndOfferId(owner: String, offerId: Long): List<OfferSearch>

    //TODO jpql is wrong, it can't differentiate events. will be fixed
    @Query("SELECT os FROM OfferSearch os WHERE os.offerId IN ( SELECT inos.offerId FROM OfferSearch inos left join inos.events e GROUP BY inos.offerId, inos.owner HAVING COUNT(inos) > 1 )")
    fun findAllDiff(): List<OfferSearch>

}
