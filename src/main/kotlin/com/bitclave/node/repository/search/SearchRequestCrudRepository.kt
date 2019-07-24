package com.bitclave.node.repository.search

import com.bitclave.node.repository.models.SearchRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
interface SearchRequestCrudRepository : PagingAndSortingRepository<SearchRequest, Long> {

    fun findAllBy(pageable: Pageable): Slice<SearchRequest>

    fun findByOwnerIn(owners: List<String>, pageable: Pageable): Slice<SearchRequest>

    fun findById(id: Long): List<SearchRequest>

    fun findByOwner(owner: String): List<SearchRequest>

    fun deleteByIdAndOwner(id: Long, owner: String): Long

    @Modifying
    @Query(
        value = """
            DELETE FROM SearchRequest sr WHERE sr.owner = ?1
        """
    )
    fun deleteByOwner(owner: String): Int

    @Modifying
    @Query(
        value = """
            DELETE FROM SearchRequest sr WHERE sr.id IN ?1
        """
    )
    fun deleteByIdIn(ids: List<Long>): Int

    fun findByIdAndOwner(id: Long, owner: String): SearchRequest?

    @Query("FROM SearchRequest s JOIN  s.tags t WHERE s.owner = :owner and KEY(t) = :tagKey")
    fun getRequestByOwnerAndTag(
        @Param("owner") owner: String,
        @Param("tagKey") tagKey: String
    ): List<SearchRequest>

    @Modifying
    @Query(
        value = """
            DELETE FROM search_request_tags srt WHERE srt.search_request_id IN
            ( SELECT id FROM search_request sr WHERE sr.owner = ?1 )
        """,
        nativeQuery = true
    )
    fun deleteTagsByOwner(owner: String): Int

    @Modifying
    @Query(
        value = """
            DELETE FROM search_request_tags srt WHERE srt.search_request_id IN ?
        """,
        nativeQuery = true
    )
    fun deleteTagsByIdIn(ids: List<Long>): Int

    @Query(
        value = """
            select sr.* from
            ( select sr.owner, srt.tags_key, count(sr.id)
            from search_request sr join search_request_tags srt on srt.search_request_id = sr.id
            group by sr.owner, srt.tags_key
            having count(sr.id) > 1) a,
            search_request sr
            join search_request_tags srt on srt.search_request_id = sr.id
            where sr.owner = a.owner and srt.tags_key = a.tags_key
        """,
        nativeQuery = true
    )
    fun getSearchRequestWithSameTags(): List<SearchRequest>
}
