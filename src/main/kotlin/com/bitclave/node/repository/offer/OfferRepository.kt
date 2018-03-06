package com.bitclave.node.repository.offer

import com.bitclave.node.repository.models.Offer

interface OfferRepository {

    fun saveOffer(offer: Offer): Offer

    fun findByOwner(owner: String): List<Offer>

    fun findByIdAndOwner(id: Long, owner: String): Offer?

}
