package com.bitclave.node.repository.models

import javax.persistence.*
import java.util.Date

@Entity
open class OfferSearch(
        @GeneratedValue(strategy = GenerationType.TABLE) @Id val id: Long = 0,
        val searchRequestId: Long = 0,
        val offerId: Long = 0,
        var state: OfferResultAction = OfferResultAction.NONE,
        var lastUpdated: String = Date().toString(),
        var lastUpdate: Date = Date(),
        @Column(length=4096) var info: String = "[]",
//        @Column(length=10485760) var events: String = ""
        @ElementCollection(fetch = FetchType.EAGER) var events: MutableList<String> = ArrayList()
) {

}

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
