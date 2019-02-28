package com.bitclave.node.repository.models

import javax.persistence.*

@Entity
data class QuerySearchRequest(

        @GeneratedValue(strategy = GenerationType.TABLE)
        @Id
        val id: Long = 0,

        @Column(length = 256)
        val owner: String = "",

        @Column(length = 256)
        val query: String = ""
)
