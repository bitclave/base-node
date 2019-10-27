package com.bitclave.node.repository.entities

import com.bitclave.node.configuration.gson.Exclude
import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.Parameter
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity
data class Site(
    @Exclude
    @GenericGenerator(
        name = "site_seq",
        strategy = "sequence",
        parameters = [
            Parameter(name = "sequence_name", value = "site_id_seq"),
            Parameter(name = "initial_value", value = "20"),
            Parameter(name = "increment_size", value = "1")
        ]
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "site_seq")
    @Id val id: Long = 0,
    val origin: String = "",
    val publicKey: String = "",
    val confidential: Boolean = false
)
