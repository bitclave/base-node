package com.bitclave.node.repository.search.offer

import com.bitclave.node.repository.models.OfferResultAction
import com.bitclave.node.repository.models.OfferSearch
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
interface OfferSearchCrudRepository : PagingAndSortingRepository<OfferSearch, Long> {

    fun deleteAllBySearchRequestId(id: Long): Long

    fun deleteAllByOwner(owner: String): List<Long>

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

    fun findAllByOwnerAndStateIn(owner: String, state: List<OfferResultAction>): List<OfferSearch>

    fun findAllByOwnerAndSearchRequestIdIn(owner: String, searchIds: List<Long>): List<OfferSearch>

    fun findByOwnerAndOfferId(owner: String, offerId: Long): List<OfferSearch>

    fun findByOwnerAndOfferIdIn(owner: String, offerIds: List<Long>): List<OfferSearch>

    fun findByOwnerAndSearchRequestIdInAndStateIn(
        owner: String,
        searchIds: List<Long>,
        state: List<OfferResultAction>
    ): List<OfferSearch>

    @Query(
        value = """
            SELECT *, CASE WHEN r.rank IS NULL THEN 0 ELSE r.rank END AS united_rank
            FROM offer_search s LEFT JOIN offer_rank r ON s.offer_id = r.offer_id
            WHERE s.owner = :owner
            order by united_rank desc, s.updated_at DESC
        """,
        nativeQuery = true
    )
    fun getOfferSearchByOwnerAndSortByRank(@Param("owner") owner: String): List<OfferSearch>

    @Query(
        value = """
            SELECT *
            FROM offer_search s, offer o WHERE s.offer_id = o.id AND s.owner = :owner
            ORDER BY o.updated_at DESC
        """,
        nativeQuery = true
    )
    fun getOfferSearchByOwnerAndSortByUpdatedAt(@Param("owner") owner: String): List<OfferSearch>

    @Query(
        value = """
            SELECT DISTINCT
                first_value( CAST( p.worth AS INT ) ) over (partition by s.id order by p.id),
                s.*
            FROM offer_search s JOIN offer_price p ON p.offer_id = s.offer_id
            WHERE s.owner = :owner
            ORDER BY first_value DESC
        """,
        nativeQuery = true
    )
    fun getOfferSearchByOwnerAndSortByOfferPriceWorth(@Param("owner") owner: String): List<OfferSearch>

    @Query(
        value = """
            SELECT CAST( t.tags AS FLOAT ), *
            FROM offer_search s JOIN offer_tags t ON t.offer_id = s.offer_id
            where t.tags_key = 'cashback' AND s.owner = :owner
            order by t.tags DESC
        """,
        nativeQuery = true
    )
    fun getOfferSearchByOwnersAndSortByCashBack(@Param("owner") owner: String): List<OfferSearch>

    @Query(
        value = """
            SELECT *, CASE WHEN r.rank IS NULL THEN 0 ELSE r.rank END AS united_rank
            FROM offer_search s LEFT JOIN offer_rank r ON s.offer_id = r.offer_id
            WHERE s.owner = :owner AND s.state IN :state
            order by united_rank desc, s.updated_at DESC
        """,
        nativeQuery = true
    )
    fun getOfferSearchByOwnerAndStateSortByRank(
        @Param("owner") owner: String,
        @Param("state") state: List<Long>
    ): List<OfferSearch>

    @Query(
        value = """
            SELECT *
            FROM offer_search s, offer o WHERE s.offer_id = o.id AND s.owner = :owner AND s.state IN :state
            order by o.updated_at DESC
        """,
        nativeQuery = true
    )
    fun getOfferSearchByOwnerAndStateSortByUpdatedAt(
        @Param("owner") owner: String,
        @Param("state") state: List<Long>
    ): List<OfferSearch>

    @Query(
        value = """
            SELECT DISTINCT
                first_value( CAST( p.worth AS INT ) ) over (partition by s.id order by p.id),
                s.*
            FROM offer_search s JOIN offer_price p ON p.offer_id = s.offer_id
            WHERE s.owner = :owner AND s.state IN :state
            ORDER BY first_value DESC
        """,
        nativeQuery = true
    )
    fun getOfferSearchByOwnerAndStateAndSortByOfferPriceWorth(
        @Param("owner") owner: String,
        @Param("state") state: List<Long>
    ): List<OfferSearch>

    @Query(
        value = """
            SELECT CAST( t.tags AS FLOAT ), *
            FROM offer_search s JOIN offer_tags t ON t.offer_id = s.offer_id
            where t.tags_key = 'cashback' AND s.owner = :owner AND s.state IN :state
            order by t.tags DESC
        """,
        nativeQuery = true
    )
    fun getOfferSearchByOwnerAndStateAndSortByCashBack(
        @Param("owner") owner: String,
        @Param("state") state: List<Long>
    ): List<OfferSearch>

    @Query(
        value = """
            SELECT *, CASE WHEN r.rank IS NULL THEN 0 ELSE r.rank END AS united_rank
            FROM offer_search s LEFT JOIN offer_rank r ON s.offer_id = r.offer_id
            where s.owner = :owner AND s.search_request_id IN :ids
            order by united_rank desc, s.updated_at DESC
        """,
        nativeQuery = true
    )
    fun getOfferSearchByOwnerAndSearchRequestIdInSortByRank(
        @Param("owner") owner: String,
        @Param("ids") searchRequestIds: List<Long>
    ): List<OfferSearch>

    @Query(
        value = """
            SELECT *
            FROM offer_search s, offer o WHERE s.offer_id = o.id AND s.owner = :owner AND s.search_request_id IN :ids
            ORDER BY o.updated_at DESC
        """,
        nativeQuery = true
    )
    fun getOfferSearchByOwnerAndSearchRequestIdInSortByUpdatedAt(
        @Param("owner") owner: String,
        @Param("ids") searchRequestIds: List<Long>
    ): List<OfferSearch>

    @Query(
        value = """
            SELECT CAST( t.tags AS FLOAT ), *
            FROM offer_search s JOIN offer_tags t ON t.offer_id = s.offer_id
            where t.tags_key = 'cashback' AND s.owner = :owner AND s.search_request_id IN :ids
            order by t.tags DESC
        """,
        nativeQuery = true
    )
    fun getOfferSearchByOwnerAndSearchRequestIdInAndSortByCashback(
        @Param("owner") owner: String,
        @Param("ids") searchRequestIds: List<Long>
    ): List<OfferSearch>

    // the cluster of third requests
    // by (Owner AND SearchRequests AND States)
    // the differences are only sorting

    @Query(
        value = """
            SELECT *
            FROM offer_search s, offer o
            WHERE
                s.offer_id = o.id
                AND s.owner = :owner
                AND s.search_request_id IN :ids
                AND s.state IN :state
            ORDER BY o.updated_at DESC
        """,
        nativeQuery = true
    )
    fun getOfferSearchByOwnerAndSearchRequestIdInAndStateSortByUpdatedAt(
        @Param("owner") owner: String,
        @Param("ids") searchRequestIds: List<Long>,
        @Param("state") state: List<Long>
    ): List<OfferSearch>

    @Query(
        value = """
            SELECT *, CASE WHEN r.rank IS NULL THEN 0 ELSE r.rank END AS united_rank
            FROM offer_search s LEFT JOIN offer_rank r ON s.offer_id = r.offer_id
            WHERE s.owner = :owner AND s.search_request_id IN :ids AND s.state IN :state
            ORDER BY united_rank desc, s.updated_at DESC
        """,
        nativeQuery = true
    )
    fun getOfferSearchByOwnerAndSearchRequestIdInAndStateSortByRank(
        @Param("owner") owner: String,
        @Param("ids") searchRequestIds: List<Long>,
        @Param("state") state: List<Long>
    ): List<OfferSearch>

    @Query(
        value = """
            SELECT DISTINCT
                first_value( CAST( p.worth AS INT ) ) over (partition by s.id order by p.id),
                s.*
            FROM offer_search s JOIN offer_price p ON p.offer_id = s.offer_id
            WHERE s.owner = :owner AND s.search_request_id IN :ids
            ORDER BY first_value DESC
        """,
        nativeQuery = true
    )
    fun getOfferSearchByOwnerAndSearchRequestIdInAndSortByOfferPriceWorth(
        @Param("owner") owner: String,
        @Param("ids") searchRequestIds: List<Long>
    ): List<OfferSearch>

    @Query(
        value = """
            SELECT DISTINCT
                first_value( CAST( p.worth AS INT ) ) over (partition by s.id order by p.id),
                s.*
            FROM offer_search s JOIN offer_price p ON p.offer_id = s.offer_id
            WHERE s.owner = :owner AND s.search_request_id IN :ids AND s.state IN :state
            ORDER BY first_value DESC
        """,
        nativeQuery = true
    )
    fun getOfferSearchByOwnerAndSearchRequestIdInAndStateSortByOfferPriceWorth(
        @Param("owner") owner: String,
        @Param("ids") searchRequestIds: List<Long>,
        @Param("state") state: List<Long>
    ): List<OfferSearch>

    @Query(
        value = """
            SELECT CAST( t.tags AS FLOAT ), *
            FROM offer_search s JOIN offer_tags t ON t.offer_id = s.offer_id
            where t.tags_key = 'cashback' AND s.owner = :owner AND s.search_request_id IN :ids AND s.state IN :state
            order by t.tags DESC
        """,
        nativeQuery = true
    )
    fun getOfferSearchByOwnerAndSearchRequestIdAndStateSortByCashback(
        @Param("owner") owner: String,
        @Param("ids") searchRequestIds: List<Long>,
        @Param("state") state: List<Long>
    ): List<OfferSearch>

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
