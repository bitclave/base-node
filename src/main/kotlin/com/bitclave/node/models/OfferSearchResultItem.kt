package com.bitclave.node.models

import com.bitclave.node.repository.entities.Offer
import com.bitclave.node.repository.entities.OfferInteraction
import com.bitclave.node.repository.entities.OfferSearch

data class OfferSearchResultItem(
    val offerSearch: OfferSearch,
    val offer: Offer,
    val interaction: OfferInteraction?
)
