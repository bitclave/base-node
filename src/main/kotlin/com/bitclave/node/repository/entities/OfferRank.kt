package com.bitclave.node.repository.entities

import org.hibernate.annotations.ColumnDefault
import org.springframework.format.annotation.DateTimeFormat
import java.util.Date
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity
data class OfferRank(
    @GeneratedValue(strategy = GenerationType.TABLE) @Id val id: Long = 0,
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