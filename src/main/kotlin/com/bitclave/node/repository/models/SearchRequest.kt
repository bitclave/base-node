package com.bitclave.node.repository.models

import org.springframework.format.annotation.DateTimeFormat
import java.util.Date
import java.util.HashMap
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity
data class SearchRequest(
    @GeneratedValue(strategy = GenerationType.TABLE) @Id val id: Long = 0,
    val owner: String = "",
    @ElementCollection(fetch = FetchType.LAZY) val tags: Map<String, String> = HashMap(),

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    val createdAt: Date = Date(),
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    val updatedAt: Date = Date()
)
