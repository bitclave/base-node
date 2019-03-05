package com.bitclave.node.repository.models

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

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
