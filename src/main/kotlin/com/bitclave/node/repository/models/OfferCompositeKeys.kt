package com.bitclave.node.repository.models

import java.io.Serializable


data class OfferCompositeKeys(
        val offerId: Long = 0,
        val clientId: String = "",
        val offerOwner: String = ""
) : Serializable
