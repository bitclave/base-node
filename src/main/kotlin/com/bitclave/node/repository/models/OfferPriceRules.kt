package com.bitclave.node.repository.models

import com.bitclave.node.configuration.gson.Exclude
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

@Entity
data class OfferPriceRules(
    @GeneratedValue(strategy = GenerationType.TABLE) @Id
    val id: Long = 0,

    @Column(length = 256)
    val rulesKey: String = "",

    @Column(length = 256) val
    value: String = "",

    var rule: Offer.CompareAction = Offer.CompareAction.EQUALLY,

    @Exclude
    @Column(name = "offer_price_id", insertable = false, updatable = false)
    val originalOfferPriceId: Long = 0
) {
    @Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offer_price_id")
    var offerPrice: OfferPrice? = null
}
