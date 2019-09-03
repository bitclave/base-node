package com.bitclave.node.repository.entities

import com.bitclave.node.configuration.gson.Exclude
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity
data class Site(
    @Exclude @GeneratedValue(strategy = GenerationType.TABLE) @Id val id: Long = 0,
    val origin: String = "",
    val publicKey: String = "",
    val confidential: Boolean = false
)
