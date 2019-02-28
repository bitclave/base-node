package com.bitclave.node.repository.models

import com.bitclave.node.configuration.gson.Exclude
import org.hibernate.annotations.ColumnDefault
import javax.persistence.*

@Entity
data class UploadedFile(
        @GeneratedValue(strategy = GenerationType.TABLE) @Id
        val id: Long = 0,

        val publicKey: String = "",

        @Column(length = 256)
        var name: String = "",

        @Column(length = 256)
        var mimeType: String = "",

        @Column(nullable = false) @ColumnDefault("0")
        var size: Long = 0,

        @Exclude
        @Lob
        var data: ByteArray? = null
)
