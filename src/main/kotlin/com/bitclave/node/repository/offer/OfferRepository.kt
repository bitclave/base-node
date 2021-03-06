package com.bitclave.node.repository.offer

import com.bitclave.node.repository.entities.Offer
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice

interface OfferRepository {

    fun saveOffer(offer: Offer): Offer

    fun saveAll(offers: List<Offer>): List<Offer>

    fun deleteOffer(id: Long, owner: String): Long

    fun deleteOffers(owner: String): Int

    fun findIdsByOwner(owner: String): List<Long>

    fun findByOwner(owner: String): List<Offer>

    fun findByOwner(owner: String, pageable: Pageable): Page<Offer>

    fun getAllOffersExceptProducts(pageable: Pageable): Page<Offer>

    fun getAllOffersSlice(
        pageable: Pageable,
        syncCompare: Boolean,
        syncRules: Boolean,
        syncPrices: Boolean,
        exceptType: Offer.OfferType?
    ): Slice<Offer>

    fun findByIds(ids: List<Long>, pageable: Pageable): Page<Offer>

    fun findByIds(ids: List<Long>): List<Offer>

    fun findById(id: Long): Offer?

    fun findByIdAndOwner(id: Long, owner: String): Offer?

    fun findAll(): List<Offer>

    fun findAll(pageable: Pageable): Page<Offer>

    fun getTotalCount(): Long

    fun getOfferByOwnerAndTag(owner: String, tagKey: String): List<Offer>

    fun findAllWithoutOwner(): List<Offer>
}
