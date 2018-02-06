package com.bitclave.node.repository.models

import com.fasterxml.jackson.annotation.JsonIgnore
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Transient

@Entity
data class Account(
        @Id val id: String = "",
        @Column(length = 256) val publicKey: String = "",
        @Transient val hash: String = ""
) {
    @JsonIgnore
    fun isValid(): Boolean = hash.length == 64 && publicKey.length == 66
}
