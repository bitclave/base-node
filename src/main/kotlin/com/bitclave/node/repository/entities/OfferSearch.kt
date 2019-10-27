package com.bitclave.node.repository.entities

import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.Parameter
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
    @GenericGenerator(
        name = "offer_search_seq",
        strategy = "sequence",
        parameters = [
            Parameter(name = "sequence_name", value = "offer_search_id_seq"),
            Parameter(name = "initial_value", value = "42798414"),
            Parameter(name = "increment_size", value = "1")
        ]
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "offer_search_seq")
    @Id val id: Long = 0,

    @Column(length = 256) var owner: String = "",

    val searchRequestId: Long = 0,

    val offerId: Long = 0,

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) val createdAt: Date = Date()
) {
    fun hashCodeByOfferIdAndOwner(): Int {
        var result = owner.hashCode()
        result = 31 * result + offerId.hashCode()

        return result
    }
}
