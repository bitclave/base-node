package com.bitclave.node.repository.models

import com.bitclave.node.configuration.gson.Exclude
import com.bitclave.node.utils.KeyPairUtils
import org.hibernate.annotations.ColumnDefault
import org.springframework.format.annotation.DateTimeFormat
import java.util.Date
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class Account(
    @Column(length = 256, unique = true) @Id val publicKey: String = "",
    @Column(nullable = false) @ColumnDefault("0") var nonce: Long = 0,

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    val createdAt: Date = Date(),

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    val updatedAt: Date = Date()
) {
    @Exclude
    fun isValid(): Boolean = KeyPairUtils.isValidPublicKey(this.publicKey)
}
