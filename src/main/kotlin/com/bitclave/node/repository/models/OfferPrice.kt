package com.bitclave.node.repository.models

import com.bitclave.node.configuration.gson.Exclude
import org.hibernate.annotations.ColumnDefault
import java.math.BigDecimal
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany

@Entity
data class OfferPrice(
    @GeneratedValue(strategy = GenerationType.TABLE) @Id
    val id: Long = 0,

    @Column(length = 256)
    var description: String = "",

    @ColumnDefault("0")
    var worth: String = BigDecimal.ZERO.toString(),

    @OneToMany(mappedBy = "offerPrice", cascade = [CascadeType.REMOVE], fetch = FetchType.LAZY)
    var rules: List<OfferPriceRules> = emptyList(),

    @Exclude
    @Column(name = "offer_id", insertable = false, updatable = false)
    val originalOfferId: Long = 0
) {
    @Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offer_id")
    var offer: Offer? = null
}
