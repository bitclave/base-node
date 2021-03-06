package com.bitclave.node.repository.entities

import com.bitclave.node.configuration.gson.Exclude
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.Parameter
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
    @GenericGenerator(
        name = "offer_price_seq",
        strategy = "sequence",
        parameters = [
            Parameter(name = "sequence_name", value = "offer_price_id_seq"),
            Parameter(name = "initial_value", value = "25766485"),
            Parameter(name = "increment_size", value = "1")
        ]
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "offer_price_seq")
    @Id
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
