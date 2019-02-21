package com.bitclave.node.repository.models

import com.bitclave.node.utils.KeyPairUtils
import org.hibernate.annotations.ColumnDefault
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class Account(
        @Column(length = 256, unique = true) @Id val publicKey: String = "",
        @Column(nullable = false) @ColumnDefault("0") var nonce: Long = 0
) {
    fun isValid(): Boolean = KeyPairUtils.isValidPublicKey(this.publicKey)
}
