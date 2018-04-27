package com.bitclave.node.repository.models

import com.fasterxml.jackson.annotation.JsonIgnore
import org.hibernate.annotations.ColumnDefault
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class Account(
        @Column(length = 256, unique = true) @Id val publicKey: String = "",
        @Column(nullable = false) @ColumnDefault("0") var nonce: Long = 0
) {
    @JsonIgnore
    fun isValid(): Boolean = publicKey.length == 66
}
