package com.bitclave.node.repository.models

import org.springframework.format.annotation.DateTimeFormat
import java.util.*
import javax.persistence.*

@Entity
data class SearchRequest(
        @GeneratedValue(strategy = GenerationType.TABLE) @Id val id: Long = 0,
        val owner: String = "",
        @ElementCollection(fetch = FetchType.EAGER) val tags: Map<String, String> = HashMap(),

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        val createdAt: Date = Date(),
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        val updatedAt: Date = Date()
)
