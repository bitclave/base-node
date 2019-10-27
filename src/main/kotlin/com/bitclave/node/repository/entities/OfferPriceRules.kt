package com.bitclave.node.repository.entities

import com.bitclave.node.configuration.gson.Exclude
import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.Parameter
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
    @GenericGenerator(
        name = "offer_price_rules_seq",
        strategy = "sequence",
        parameters = [
            Parameter(name = "sequence_name", value = "offer_price_rules_id_seq"),
            Parameter(name = "initial_value", value = "25766711"),
            Parameter(name = "increment_size", value = "1")
        ]
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "offer_price_rules_seq")
    @Id
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
