package com.bitclave.node.repository.models

import javax.persistence.*

@Entity
data class SearchRequest(
        @GeneratedValue(strategy = GenerationType.TABLE) @Id val id: Long = 0,
        val owner: String = "",
        @ElementCollection(fetch = FetchType.EAGER) val tags: Map<String, String> = HashMap()
)
