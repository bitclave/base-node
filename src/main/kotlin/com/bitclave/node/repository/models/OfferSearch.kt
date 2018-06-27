package com.bitclave.node.repository.models

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity
open class OfferSearch(
        @GeneratedValue(strategy = GenerationType.TABLE) @Id val id: Long = 0,
        val searchRequestId: Long = 0,
        val offerId: Long = 0,
        var state: OfferResultAction = OfferResultAction.NONE
)

enum class OfferResultAction {
    NONE,
    ACCEPT,
    REJECT
}
