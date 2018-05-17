package com.bitclave.node.repository.offer

import com.bitclave.node.repository.models.Offer

interface OfferRepository {

    fun saveOffer(offer: Offer): Offer

    fun deleteOffer(id: Long, owner: String): Long

    fun deleteOffers(owner: String): Long

    fun findByOwner(owner: String): List<Offer>

    fun findById(ids: List<Long>): List<Offer>

    fun findById(id: Long): Offer?

    fun findByIdAndOwner(id: Long, owner: String): Offer?

    fun findAll(): List<Offer>

}
