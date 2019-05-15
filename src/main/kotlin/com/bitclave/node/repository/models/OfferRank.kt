package com.bitclave.node.repository.models

import org.hibernate.annotations.ColumnDefault
import org.springframework.format.annotation.DateTimeFormat
import java.util.*
import javax.persistence.*

@Entity
data class OfferRank(
    @GeneratedValue(strategy = GenerationType.TABLE) @Id val id: Long = 0,
    @ColumnDefault("0") var rank: Long = 0,

    @Column val offerId: Long = 0,
    @Column val rankerId: Long = 0,

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    val createdAt: Date = Date(),

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    val updatedAt: Date = Date()
)
