package com.bitclave.node.repository.models

import org.hibernate.annotations.ColumnDefault
import java.math.BigDecimal
import java.util.*
import javax.persistence.*

@Entity
data class Offer(
        @GeneratedValue(strategy = GenerationType.TABLE) @Id val id: Long = 0,
        @Column(length = 256) val owner: String = "",

        @OneToMany(mappedBy = "offer", cascade = [CascadeType.REMOVE], fetch = FetchType.EAGER)
        var offerPrices: List<OfferPrice> = emptyList(),

        @Column(length = 512) val description: String = "",
        @Column(length = 256) val title: String = "",
        @Column(length = 512) val imageUrl: String = "",

        @ColumnDefault("0") val worth: String = BigDecimal.ZERO.toString(),

        @ElementCollection(fetch = FetchType.EAGER) val tags: Map<String, String> = HashMap(),
        @ElementCollection(fetch = FetchType.EAGER) val compare: Map<String, String> = HashMap(),
        @ElementCollection(fetch = FetchType.EAGER) val rules: Map<String, CompareAction> = HashMap(),

        @Column(columnDefinition = "timestamp")
        val createdAt: Date = Date(),
        @Column(columnDefinition = "timestamp")
        val updatedAt: Date = Date()
) {

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
