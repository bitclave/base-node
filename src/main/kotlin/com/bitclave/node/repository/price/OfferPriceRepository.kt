package com.bitclave.node.repository.price

import com.bitclave.node.repository.models.Offer
import com.bitclave.node.repository.models.OfferPrice

interface OfferPriceRepository {
    fun savePrices(offer: Offer, prices: List<OfferPrice>): List<OfferPrice>
}