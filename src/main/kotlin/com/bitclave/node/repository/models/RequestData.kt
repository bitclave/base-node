package com.bitclave.node.repository.models

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity
data class RequestData(
    @GeneratedValue(strategy = GenerationType.TABLE) @Id val id: Long = 0,
    val fromPk: String = "",
    val toPk: String = "",
    @Column(columnDefinition = "TEXT") val requestData: String = "",
    @Column(columnDefinition = "TEXT") val responseData: String = ""
)
