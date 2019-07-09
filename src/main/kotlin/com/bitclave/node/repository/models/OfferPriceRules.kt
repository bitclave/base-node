package com.bitclave.node.repository.models

import com.bitclave.node.configuration.gson.Exclude
import javax.persistence.Column
import javax.persistence.Entity
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

    var rule: Offer.CompareAction = Offer.CompareAction.EQUALLY
) {
    @Exclude
    @ManyToOne
    @JoinColumn(name = "offer_price_id")
    var offerPrice: OfferPrice? = null
}
