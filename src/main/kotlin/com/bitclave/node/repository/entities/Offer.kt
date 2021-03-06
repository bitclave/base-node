package com.bitclave.node.repository.entities

import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.Parameter
import org.springframework.format.annotation.DateTimeFormat
import java.math.BigDecimal
import java.util.Date
import java.util.HashMap
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.MapKeyColumn
import javax.persistence.OneToMany

@Entity
data class Offer(
    @GenericGenerator(
        name = "offer_seq",
        strategy = "sequence",
        parameters = [
            Parameter(name = "sequence_name", value = "offer_id_seq"),
            Parameter(name = "initial_value", value = "17239325"),
            Parameter(name = "increment_size", value = "1")
        ]
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "offer_seq")
    @Id val id: Long = 0,

    @Column(length = 256) val owner: String = "",

    @OneToMany(mappedBy = "offer", cascade = [CascadeType.REMOVE], fetch = FetchType.LAZY)
    val offerPrices: List<OfferPrice> = emptyList(),

    @Column(length = 512) val description: String = "",
    @Column(length = 256) val title: String = "",
    @Column(length = 512) val imageUrl: String = "",

    @ColumnDefault("0") val worth: String = BigDecimal.ZERO.toString(),

    @ElementCollection(fetch = FetchType.LAZY)
    @MapKeyColumn(length = 256)
    @Column(length = 1024)
    val tags: Map<String, String> = HashMap(),

    @ElementCollection(fetch = FetchType.LAZY)
    val compare: Map<String, String> = HashMap(),

    @ElementCollection(fetch = FetchType.LAZY)
    val rules: Map<String, CompareAction> = HashMap(),

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    val createdAt: Date = Date(),

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    val updatedAt: Date = Date()
) {

    enum class OfferType {
        PRODUCT,
    }

    enum class CompareAction(
        val value: String
    ) {

        EQUALLY("="),
        NOT_EQUAL("!="),
        LESS_OR_EQUAL("<="),
        MORE_OR_EQUAL(">="),
        MORE(">"),
        LESS("<");
    }
}
