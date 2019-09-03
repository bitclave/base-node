package com.bitclave.node.repository.entities

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
    @Column(columnDefinition = "varchar(255) default '' not null") val rootPk: String = "",
    @Column(columnDefinition = "TEXT") val requestData: String = "",
    @Column(columnDefinition = "TEXT") val responseData: String = ""
)