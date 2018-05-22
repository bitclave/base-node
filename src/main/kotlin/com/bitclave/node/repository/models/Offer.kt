package com.bitclave.node.repository.models

import org.hibernate.annotations.ColumnDefault
import java.math.BigDecimal
import javax.persistence.*

@Entity
data class Offer(
        @GeneratedValue(strategy = GenerationType.TABLE) @Id val id: Long = 0,
        @Column(length = 256) val owner: String = "",
        @Column(length = 512) val description: String = "",
        @Column(length = 256) val title: String = "",
        @Column(length = 512) val imageUrl: String = "",
        @ColumnDefault("0") val worth: String = BigDecimal.ZERO.toString(),
        @ElementCollection(fetch = FetchType.EAGER) val tags: Map<String, String> = HashMap(),
        @ElementCollection(fetch = FetchType.EAGER) val compare: Map<String, String> = HashMap(),
        @ElementCollection(fetch = FetchType.EAGER) val rules: Map<String, CompareAction> = HashMap()

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
