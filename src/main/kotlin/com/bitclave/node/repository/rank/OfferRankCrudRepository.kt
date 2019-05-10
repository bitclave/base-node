package com.bitclave.node.repository.rank

import com.bitclave.node.repository.models.OfferRank
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
interface OfferRankCrudRepository: PagingAndSortingRepository<OfferRank, Long> {
    fun findAllByOffer_id(offerId: Long, pageable: Pageable): Page<OfferRank>
    fun findById(id: Long): OfferRank?
    fun deleteById(id: Long): Long
}
