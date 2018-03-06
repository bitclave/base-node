package com.bitclave.node.repository.models

import javax.persistence.*

@Entity
data class Offer(
        @GeneratedValue(strategy = GenerationType.TABLE) @Id val id: Long = 0,
        val owner: String = "",
        val description: String = "",
        val title: String = "",
        val imageUrl: String = "",
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

