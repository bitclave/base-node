package com.bitclave.node.repository.offer

import com.bitclave.node.repository.entities.Offer
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.math.BigInteger

@Repository
@Transactional
interface OfferCrudRepository : PagingAndSortingRepository<Offer, Long> {

    fun findAllByIdIn(ids: List<Long>, pageable: Pageable): Page<Offer>

    @Query("SELECT id FROM Offer WHERE owner = :owner", nativeQuery = true)
    fun findIdsByOwner(@Param("owner") owner: String): List<BigInteger>

    fun findByOwner(owner: String): List<Offer>

    fun findByOwner(owner: String, pageable: Pageable): Page<Offer>

    fun deleteByIdAndOwner(id: Long, owner: String): Long

    fun deleteByOwner(owner: String): List<Offer>

    fun findByIdAndOwner(id: Long, owner: String): Offer?

    @Query("FROM Offer o JOIN  o.tags t WHERE o.owner = :owner and KEY(t) = :tagKey")
    fun getOfferByOwnerAndTag(@Param("owner") owner: String, @Param("tagKey") tagKey: String): List<Offer>

    @Query(
        value = """
            select * from offer o
            where not exists
            (select 1 from offer_tags ot where o.id = ot.offer_id and ot.tags_key = 'product' and ot.tags = 'true')
        """,
        countQuery = """
            select count(0) from offer o
            where not exists
            (select 1 from offer_tags ot where o.id = ot.offer_id and ot.tags_key = 'product' and ot.tags = 'true')
        """,
        nativeQuery = true
    )
    fun getAllOffersExceptProducts(@Param("pageable") pageable: Pageable): Page<Offer>

    @Query(
        value = """
            select * from offer o
            where not exists
            (select 1 from offer_tags ot where o.id = ot.offer_id and ot.tags_key = 'product' and ot.tags = 'true')
        """,
        nativeQuery = true
    )
    fun getAllOffersExceptProductsSlice(pageable: Pageable): Slice<Offer>

    fun getAllOffersBy(pageable: Pageable): Slice<Offer>
}
