package com.bitclave.node.repository.search.offer

import com.bitclave.node.repository.models.OfferSearch
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
interface OfferSearchCrudRepository : PagingAndSortingRepository<OfferSearch, Long> {

    @Modifying
    @Query(
        value = """
            DELETE FROM OfferSearch os WHERE os.searchRequestId = ?1
        """
    )
    fun deleteAllBySearchRequestId(id: Long): Int

    @Modifying
    @Query(
        value = """
            DELETE FROM OfferSearch os WHERE os.searchRequestId IN ?1
        """
    )
    fun deleteAllBySearchRequestIdIn(ids: List<Long>): Int

    @Modifying
    @Query(
        value = """
            DELETE FROM OfferSearch os WHERE os.owner = ?1
        """
    )
    fun deleteAllByOwner(owner: String): Int

    @Modifying
    @Query(
        value = """
            DELETE FROM OfferSearch os WHERE os.offerId = ?1
        """
    )
    fun deleteAllByOfferId(id: Long): Int

    fun findBySearchRequestId(id: Long): List<OfferSearch>

    fun findBySearchRequestId(id: Long, pageable: Pageable): Page<OfferSearch>

    fun findBySearchRequestIdIn(ids: List<Long>): List<OfferSearch>

    fun findByOfferId(id: Long): List<OfferSearch>

    fun findBySearchRequestIdAndOfferId(searchRequestId: Long, offerId: Long): List<OfferSearch>

    fun findBySearchRequestIdAndOfferIdIn(
        searchRequestId: Long,
        offerIds: List<Long>
    ): List<OfferSearch>

    fun findBySearchRequestIdInAndOwner(searchRequestIds: List<Long>, owner: String): List<OfferSearch>

    fun findByOwner(owner: String): List<OfferSearch>

    @Query(
        value = """
            SELECT * FROM offer_search s
            JOIN offer_interaction ss on s.offer_id = ss.offer_id AND s.owner = ss.owner
            WHERE s.owner = :owner and ss.state in (:state)
            order by ss.updated_at DESC
        """,
        nativeQuery = true
    )
    fun findAllByOwnerAndStateIn(@Param("owner") owner: String, @Param("state") state: List<Long>): List<OfferSearch>

    fun findAllByOwnerAndSearchRequestIdIn(owner: String, searchIds: List<Long>): List<OfferSearch>

    fun findByOwnerAndOfferId(owner: String, offerId: Long): List<OfferSearch>

    fun findByOwnerAndOfferIdIn(owner: String, offerIds: List<Long>): List<OfferSearch>

    @Query(
        value = """
            SELECT * FROM offer_search s
            JOIN offer_interaction ss on s.offer_id = ss.offer_id AND s.owner = ss.owner
            WHERE s.owner = :owner and ss.state in (:state) and s.search_request_id in (:searchIds)
            order by ss.updated_at DESC
        """,
        nativeQuery = true
    )
    fun findByOwnerAndSearchRequestIdInAndStateIn(
        @Param("owner") owner: String,
        @Param("searchIds") searchIds: List<Long>,
        @Param("state") state: List<Long>
    ): List<OfferSearch>

    @Query(
        value = """
            SELECT *, CASE WHEN r.rank IS NULL THEN 0 ELSE r.rank END AS united_rank,
            CASE WHEN ss.updated_at IS NULL THEN s.created_at ELSE ss.updated_at END AS updated_at_sort
            FROM offer_search s LEFT JOIN offer_rank r ON s.offer_id = r.offer_id
            LEFT JOIN offer_interaction ss on s.offer_id = ss.offer_id AND s.owner = ss.owner
            WHERE s.owner = :owner
            order by united_rank desc, updated_at_sort DESC
        """,
        nativeQuery = true
    )
    fun getOfferSearchByOwnerAndSortByRank(@Param("owner") owner: String): List<OfferSearch>

    @Query(
        value = """
            SELECT *,
            CASE WHEN ss.updated_at IS NULL THEN s.created_at ELSE ss.updated_at END AS updated_at_sort
            FROM offer_search s
            LEFT JOIN offer_interaction ss on s.offer_id = ss.offer_id AND s.owner = ss.owner
            WHERE s.owner = :owner
            ORDER BY updated_at_sort DESC
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
            SELECT *, CAST( t.tags AS FLOAT ) AS cashback
            FROM offer_search s JOIN offer_tags t ON t.offer_id = s.offer_id
            where t.tags_key = 'cashback' AND s.owner = :owner
            order by cashback DESC
        """,
        nativeQuery = true
    )
    fun getOfferSearchByOwnersAndSortByCashBack(@Param("owner") owner: String): List<OfferSearch>

    @Query(
        value = """
            SELECT *, CASE WHEN r.rank IS NULL THEN 0 ELSE r.rank END AS united_rank
            FROM offer_search s JOIN offer_interaction ss on s.offer_id = ss.offer_id AND s.owner = ss.owner
            LEFT JOIN offer_rank r ON s.offer_id = r.offer_id
            WHERE s.owner = :owner AND ss.state IN :state
            order by united_rank desc, ss.updated_at DESC
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
            FROM offer_search s JOIN offer_interaction ss on s.offer_id = ss.offer_id AND s.owner = ss.owner
            WHERE s.owner = :owner AND ss.state IN :state
            order by ss.updated_at DESC
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
            JOIN offer_interaction ss on s.offer_id = ss.offer_id AND s.owner = ss.owner
            WHERE s.owner = :owner AND ss.state IN :state
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
            SELECT *, CAST( t.tags AS FLOAT ) AS cashback
            FROM offer_search s JOIN offer_tags t ON t.offer_id = s.offer_id
            JOIN offer_interaction ss on s.offer_id = ss.offer_id AND s.owner = ss.owner
            where t.tags_key = 'cashback' AND s.owner = :owner AND ss.state IN :state
            order by cashback DESC
        """,
        nativeQuery = true
    )
    fun getOfferSearchByOwnerAndStateAndSortByCashBack(
        @Param("owner") owner: String,
        @Param("state") state: List<Long>
    ): List<OfferSearch>

    @Query(
        value = """
            SELECT *, CASE WHEN r.rank IS NULL THEN 0 ELSE r.rank END AS united_rank,
            CASE WHEN ss.updated_at IS NULL THEN s.created_at ELSE ss.updated_at END AS updated_at_sort
            FROM offer_search s LEFT JOIN offer_rank r ON s.offer_id = r.offer_id
            LEFT JOIN offer_interaction ss on s.offer_id = ss.offer_id AND s.owner = ss.owner
            where s.owner = :owner AND s.search_request_id IN :ids
            order by united_rank desc, updated_at_sort DESC
        """,
        nativeQuery = true
    )
    fun getOfferSearchByOwnerAndSearchRequestIdInSortByRank(
        @Param("owner") owner: String,
        @Param("ids") searchRequestIds: List<Long>
    ): List<OfferSearch>

    @Query(
        value = """
            SELECT *,
            CASE WHEN ss.updated_at IS NULL THEN s.created_at ELSE ss.updated_at END AS updated_at_sort
            FROM offer_search s
            LEFT JOIN offer_interaction ss on s.offer_id = ss.offer_id AND s.owner = ss.owner
            WHERE s.owner = :owner AND s.search_request_id IN :ids
            ORDER BY updated_at_sort DESC
        """,
        nativeQuery = true
    )
    fun getOfferSearchByOwnerAndSearchRequestIdInSortByUpdatedAt(
        @Param("owner") owner: String,
        @Param("ids") searchRequestIds: List<Long>
    ): List<OfferSearch>

    @Query(
        value = """
            SELECT *, CAST( t.tags AS FLOAT ) AS cashback
            FROM offer_search s JOIN offer_tags t ON t.offer_id = s.offer_id
            where t.tags_key = 'cashback' AND s.owner = :owner AND s.search_request_id IN :ids
            order by cashback DESC
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
            FROM offer_search s
            JOIN offer_interaction ss on s.offer_id = ss.offer_id AND s.owner = ss.owner
            WHERE
                s.owner = :owner
                AND s.search_request_id IN :ids
                AND ss.state IN :state
            ORDER BY ss.updated_at DESC
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
            FROM offer_search s JOIN offer_interaction ss on s.offer_id = ss.offer_id AND s.owner = ss.owner
            LEFT JOIN offer_rank r ON s.offer_id = r.offer_id
            WHERE s.owner = :owner AND s.search_request_id IN :ids AND ss.state IN :state
            ORDER BY united_rank desc, ss.updated_at DESC
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
            JOIN offer_interaction ss on s.offer_id = ss.offer_id AND s.owner = ss.owner
            WHERE s.owner = :owner AND s.search_request_id IN :ids AND ss.state IN :state
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
            SELECT *, CAST( t.tags AS FLOAT ) AS cashback
            FROM offer_search s JOIN offer_tags t ON t.offer_id = s.offer_id
            JOIN offer_interaction ss on s.offer_id = ss.offer_id AND s.owner = ss.owner
            where t.tags_key = 'cashback' AND s.owner = :owner AND s.search_request_id IN :ids AND ss.state IN :state
            order by cashback DESC
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
            "( SELECT b.offer_id, b.owner from " +
            " ( SELECT os_inner.offer_id, os_inner.owner,  os_inner.state, e.events from " +
            "( select s.* from offer_interaction s, " +
            "( select offer_id, owner, count(*) offer_owner_count " +
            "from offer_interaction " +
            "group by offer_id, owner " +
            "having count(*) > 1 " +
            " ) c " +
            "where s.offer_id = c.offer_id " +
            " and s.owner = c.owner " +
            ") os_inner " +
            "left outer join " +
            "( select e_in.offer_interaction_id, string_agg(e_in.events, ',') events " +
            "from offer_interaction_events e_in " +
            "group by e_in.offer_interaction_id " +
            " ) e on e.offer_interaction_id = os_inner.id " +
            " GROUP BY os_inner.offer_id, os_inner.owner,  os_inner.state, e.events " +
            " HAVING COUNT(*) < 2 " +
            " ) b " +
            " GROUP BY b.offer_id, b.owner " +
            " ) a " +
            "where s.offer_id = a.offer_id " +
            "and s.owner = a.owner ",
        nativeQuery = true
    )
    fun findAllDiff(): List<OfferSearch>

    fun countBySearchRequestId(id: Long): Long

    @Query(
        value = """
            SELECT * FROM offer_search WHERE offer_id NOT IN
            ( SELECT id FROM offer o)
        """,
        nativeQuery = true
    )
    fun findAllWithoutOffer(): List<OfferSearch>

    @Query(
        value = """
            SELECT * FROM offer_search WHERE search_request_id NOT IN
            ( SELECT id FROM search_request)
        """,
        nativeQuery = true
    )
    fun findAllWithoutSearchRequest(): List<OfferSearch>

    @Query(
        value = """
            SELECT * FROM offer_search WHERE owner NOT IN
            ( SELECT public_key FROM account)
        """,
        nativeQuery = true
    )
    fun findAllWithoutOwner(): List<OfferSearch>

    @Query(
        value = """
            SELECT oo.* FROM offer_search oo
            left outer join offer_interaction oi on
            oo.offer_id = oi.offer_id AND oo.owner = oi.owner
            where oi.id is NULL
        """,
        nativeQuery = true
    )
    fun findAllWithoutOfferInteraction(): List<OfferSearch>
}
