package com.bitclave.node.repository.entities

import com.bitclave.node.configuration.gson.Exclude
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.Parameter
import org.springframework.format.annotation.DateTimeFormat
import java.util.Date
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Lob

@Entity
data class UploadedFile(
    @GenericGenerator(
        name = "uploaded_file_seq",
        strategy = "sequence",
        parameters = [
            Parameter(name = "sequence_name", value = "uploaded_file_id_seq"),
            Parameter(name = "initial_value", value = "7112352"),
            Parameter(name = "increment_size", value = "1")
        ]
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "uploaded_file_seq")
    @Id
    val id: Long = 0,

    val publicKey: String = "",

    @Column(length = 256)
    val name: String = "",

    @Column(length = 256)
    val mimeType: String = "",

    @Column(nullable = false) @ColumnDefault("0")
    val size: Long = 0,

    @Exclude
    @Lob
    val data: ByteArray? = null,

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    val createdAt: Date = Date(),

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    val updatedAt: Date = Date()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UploadedFile

        if (id != other.id) return false
        if (publicKey != other.publicKey) return false
        if (name != other.name) return false
        if (mimeType != other.mimeType) return false
        if (size != other.size) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + publicKey.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + size.hashCode()
        return result
    }
}
