package com.bitclave.node.repository.models

import org.springframework.format.annotation.DateTimeFormat
import java.util.Date
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table
import javax.persistence.UniqueConstraint

@Entity
@Table(
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["searchRequestId", "offerId"])
    ]
)
data class OfferSearch(
    @GeneratedValue(strategy = GenerationType.TABLE) @Id val id: Long = 0,

    @Column(length = 256) var owner: String = "",

    val searchRequestId: Long = 0,

    val offerId: Long = 0,

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) val createdAt: Date = Date()
)
