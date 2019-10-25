package com.bitclave.node.repository.entities

import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.Parameter
import org.springframework.format.annotation.DateTimeFormat
import java.util.Date
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity
data class QuerySearchRequest(
    @GenericGenerator(
        name = "query_search_request_seq",
        strategy = "sequence",
        parameters = [
            Parameter(name = "sequence_name", value = "query_search_request_id_seq"),
            Parameter(name = "initial_value", value = "12206704"),
            Parameter(name = "increment_size", value = "1")
        ]
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "query_search_request_seq")
    @Id
    val id: Long = 0,

    @Column(length = 256)
    val owner: String = "",

    @Column(length = 256)
    val query: String = "",

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    val createdAt: Date = Date()
)
