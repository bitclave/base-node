package com.bitclave.node.repository.models
import javax.persistence.*

@Entity
data class OfferPriceRules (
    @GeneratedValue(strategy = GenerationType.TABLE) @Id
    val id: Long = 0,

    @Column(length = 256)
    val rulesKey: String = "",

    @Column(length = 256) val
    value: String = "",

    var rule: Offer.CompareAction = Offer.CompareAction.EQUALLY,

    @ManyToOne
    @JoinColumn(name="offer_price_id")
    var offerPrice: OfferPrice? = null
)