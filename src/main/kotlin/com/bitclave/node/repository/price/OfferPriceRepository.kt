package com.bitclave.node.repository.price

import com.bitclave.node.repository.entities.Offer
import com.bitclave.node.repository.entities.OfferPrice

interface OfferPriceRepository {
    fun savePrices(offer: Offer, prices: List<OfferPrice>): List<OfferPrice>
}
