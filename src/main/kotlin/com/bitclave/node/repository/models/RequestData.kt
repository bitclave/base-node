package com.bitclave.node.repository.models

import javax.persistence.*

@Entity
data class RequestData(
        @GeneratedValue(strategy = GenerationType.TABLE) @Id val id: Long = 0,
        val fromPk: String = "",
        val toPk: String = "",
        @Column(length = 10000) val requestData: String = "",
        @Column(length = 10000) val responseData: String = ""
)
