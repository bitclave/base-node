package com.bitclave.node.repository.entities

import org.hibernate.annotations.ColumnDefault
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
data class OfferRank(
    @GenericGenerator(
        name = "offer_rank_seq",
        strategy = "sequence",
        parameters = [
            Parameter(name = "sequence_name", value = "offer_rank_id_seq"),
            Parameter(name = "initial_value", value = "3835565"),
            Parameter(name = "increment_size", value = "1")
        ]
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "offer_rank_seq")
    @Id val id: Long = 0,
    @ColumnDefault("0") var rank: Long = 0,

    @Column val offerId: Long = 0,
    @Column val rankerId: String = "",

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    val createdAt: Date = Date(),

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    val updatedAt: Date = Date()
)
