package com.bitclave.node.repository.models

import org.springframework.format.annotation.DateTimeFormat
import java.util.*
import javax.persistence.*

@Entity
@Table(uniqueConstraints = [
    UniqueConstraint(columnNames = ["searchRequestId", "offerId"])
])
open class OfferSearch(
        @GeneratedValue(strategy = GenerationType.TABLE) @Id val id: Long = 0,
        @Column(length = 256) var owner: String = "",
        val searchRequestId: Long = 0,
        val offerId: Long = 0,
        var state: OfferResultAction = OfferResultAction.NONE,
        @Column(length = 4096) var info: String = "[]",

        @ElementCollection(fetch = FetchType.EAGER) var events: MutableList<String> = ArrayList(),

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        var updatedAt: Date = Date()
)

enum class OfferResultAction {
    NONE,
    ACCEPT,         // set by ???
    REJECT,         // set by User when rejects the offer
    EVALUATE,       // set by User when following external redirect link
    CONFIRMED,      // set by Offer Owner when user completed external action
    REWARDED,       // set by Offer Owner when Owner paid out the promised reward
    COMPLAIN,       // set by User when complains on the offer
    CLAIMPURCHASE   // set by User to communicate that he mad the purchase for external offer
}
