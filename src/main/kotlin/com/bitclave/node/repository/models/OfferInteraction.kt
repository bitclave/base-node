package com.bitclave.node.repository.models

import org.springframework.format.annotation.DateTimeFormat
import java.util.Date
import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table
import javax.persistence.UniqueConstraint

enum class OfferAction {
    NONE,
    ACCEPT,         // set by ???
    REJECT,         // set by User when rejects the offer
    EVALUATE,       // set by User when following external redirect link
    CONFIRMED,      // set by Offer Owner when user completed external action
    REWARDED,       // set by Offer Owner when Owner paid out the promised reward
    COMPLAIN,       // set by User when complains on the offer
    CLAIMPURCHASE   // set by User to communicate that he mad the purchase for external offer
}

@Entity
@Table(
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["owner", "offerId"])
    ]
)
data class OfferInteraction(
    @GeneratedValue(strategy = GenerationType.TABLE) @Id val id: Long = 0,

    @Column(length = 256) val owner: String = "",

    val offerId: Long = 0,

    var state: OfferAction = OfferAction.NONE,

    @Column(length = 4096) var info: String = "[]",

    @ElementCollection(fetch = FetchType.LAZY) var events: List<String> = emptyList(),

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) var createdAt: Date = Date(),

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) var updatedAt: Date = Date()
)
