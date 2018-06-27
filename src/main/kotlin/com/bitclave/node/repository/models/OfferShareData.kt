package com.bitclave.node.repository.models

import java.math.BigDecimal
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class OfferShareData(
        @Id val offerSearchId: Long = 0,
        val offerOwner: String = "",
        val clientId: String = "",
        @Column(columnDefinition = "TEXT") val clientResponse: String = "",
        val worth: String = BigDecimal.ZERO.toString(),
        val accepted: Boolean = false
)
