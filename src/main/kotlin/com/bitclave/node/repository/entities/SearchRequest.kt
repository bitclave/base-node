package com.bitclave.node.repository.entities

import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.Parameter
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
    @GenericGenerator(
        name = "search_request_seq",
        strategy = "sequence",
        parameters = [
            Parameter(name = "sequence_name", value = "search_request_id_seq"),
            Parameter(name = "initial_value", value = "18072150"),
            Parameter(name = "increment_size", value = "1")
        ]
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "search_request_seq")
    @Id val id: Long = 0,
    val owner: String = "",
    @ElementCollection(fetch = FetchType.LAZY) val tags: Map<String, String> = HashMap(),

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    val createdAt: Date = Date(),
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    val updatedAt: Date = Date()
)
