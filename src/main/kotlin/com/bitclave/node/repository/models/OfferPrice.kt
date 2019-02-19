package com.bitclave.node.repository.models

import com.fasterxml.jackson.annotation.JsonIgnore
import org.hibernate.annotations.ColumnDefault
import java.math.BigDecimal
import javax.persistence.*

@Entity
data class OfferPrice(
        @GeneratedValue(strategy = GenerationType.TABLE) @Id
        val id: Long = 0,

        @Column(length = 256)
        var description: String = "",

        @ColumnDefault("0")
        var worth: String = BigDecimal.ZERO.toString(),

        @OneToMany(mappedBy = "offerPrice", cascade = [CascadeType.REMOVE], fetch = FetchType.EAGER)
        var rules: List<OfferPriceRules> = emptyList()
) {
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "offer_id")
    var offer: Offer? = null
}
