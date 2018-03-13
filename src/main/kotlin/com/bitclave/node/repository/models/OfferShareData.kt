package com.bitclave.node.repository.models

import java.math.BigDecimal
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.IdClass

@Entity
@IdClass(OfferCompositeKeys::class)
data class OfferShareData(
        @Id val offerId: Long = 0,
        @Id val clientId: String = "",
        @Id val offerOwner: String = "",
        @Column(length = 2000) val clientResponse: String = "",
        val worth: BigDecimal = BigDecimal.ZERO,
        val accepted: Boolean = false
)
