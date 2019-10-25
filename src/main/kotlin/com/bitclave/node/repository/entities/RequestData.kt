package com.bitclave.node.repository.entities

import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.Parameter
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity
data class RequestData(
    @GenericGenerator(
        name = "request_data_seq",
        strategy = "sequence",
        parameters = [
            Parameter(name = "sequence_name", value = "request_data_id_seq"),
            Parameter(name = "initial_value", value = "16171743"),
            Parameter(name = "increment_size", value = "1")
        ]
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "request_data_seq")
    @Id val id: Long = 0,
    val fromPk: String = "",
    val toPk: String = "",
    @Column(columnDefinition = "varchar(255) default '' not null") val rootPk: String = "",
    @Column(columnDefinition = "TEXT") val requestData: String = "",
    @Column(columnDefinition = "TEXT") val responseData: String = ""
)
