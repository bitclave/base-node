package com.bitclave.node.repository.models

import com.fasterxml.jackson.annotation.JsonIgnore
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity
data class Site(
        @JsonIgnore @GeneratedValue(strategy = GenerationType.TABLE) @Id val id: Long = 0,
        val origin: String = "",
        val publicKey: String = "",
        val confidential: Boolean = false
)
