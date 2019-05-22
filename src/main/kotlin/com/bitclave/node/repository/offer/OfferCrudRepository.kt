package com.bitclave.node.repository.offer

import com.bitclave.node.repository.models.Offer
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
interface OfferCrudRepository : PagingAndSortingRepository<Offer, Long> {

    fun findAllByIdIn(ids: List<Long>, pageable: Pageable): Page<Offer>

    fun findByOwner(owner: String): List<Offer>

    fun findByOwner(owner: String, pageable: Pageable): Page<Offer>

    fun deleteByIdAndOwner(id: Long, owner: String): Long

    fun deleteByOwner(owner: String): Long

    fun findByIdAndOwner(id: Long, owner: String): Offer?

    fun findById(id: Long): Offer?

    @Query("FROM Offer o JOIN  o.tags t WHERE o.owner = :owner and KEY(t) = :tagKey")
    fun getOfferByOwnerAndTag(@Param("owner") owner: String, @Param("tagKey") tagKey: String): List<Offer>
}
