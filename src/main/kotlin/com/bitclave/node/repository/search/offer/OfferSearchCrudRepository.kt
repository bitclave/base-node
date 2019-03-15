package com.bitclave.node.repository.search.offer

import com.bitclave.node.repository.models.OfferSearch
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
interface OfferSearchCrudRepository : PagingAndSortingRepository<OfferSearch, Long> {

    fun deleteAllBySearchRequestId(id: Long): Long

    fun findBySearchRequestId(id: Long): List<OfferSearch>

    fun findBySearchRequestId(id: Long, pageable: Pageable): Page<OfferSearch>

    fun findByOfferId(id: Long): List<OfferSearch>

    fun findBySearchRequestIdAndOfferId(searchRequestId: Long, offerId: Long): List<OfferSearch>

    fun findBySearchRequestIdAndOfferIdIn(
        searchRequestId: Long,
        offerIds: List<Long>
    ): List<OfferSearch>

    fun findBySearchRequestIdIn(searchRequestIds: List<Long>): List<OfferSearch>

    fun findByOwner(owner: String): List<OfferSearch>

    fun findByOwnerIn(owners: List<String>): List<OfferSearch>

    fun findByOwnerAndOfferId(owner: String, offerId: Long): List<OfferSearch>

    @Query(
        value = "SELECT s.* from offer_search s, " +
            "( " +
            "SELECT b.offer_id, b.owner from " +
            "(" +
            "SELECT os_inner.offer_id, os_inner.owner,  os_inner.state, e.events from " +
            "( " +
            "select s.* from offer_search s, " +
            "( " +
            "select offer_id, owner, count(*) offer_owner_count " +
            "from offer_search " +
            "group by offer_id, owner " +
            "having count(*) > 1 " +
            ") c " +
            "where s.offer_id = c.offer_id " +
            "and s.owner = c.owner " +
            ") os_inner " +
            "left outer join " +
            "( " +
            "select e_in.offer_search_id, string_agg(e_in.events, ',') events " +
            "from offer_search_events e_in " +
            "group by e_in.offer_search_id " +
            ") e on e.offer_search_id = os_inner.id " +
            "GROUP BY os_inner.offer_id, os_inner.owner,  os_inner.state, e.events " +
            "HAVING COUNT(*) < 2 " +
            ") b " +
            "GROUP BY b.offer_id, b.owner " +
            ") a " +
            "where s.offer_id = a.offer_id " +
            "and s.owner = a.owner",
        nativeQuery = true
    )
    fun findAllDiff(): List<OfferSearch>

    fun countBySearchRequestId(id: Long): Long
}
