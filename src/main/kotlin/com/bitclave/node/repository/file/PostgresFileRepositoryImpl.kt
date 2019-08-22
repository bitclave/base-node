package com.bitclave.node.repository.file

import com.bitclave.node.repository.models.UploadedFile
import com.bitclave.node.services.errors.DataNotSavedException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
@Qualifier("postgres")
class PostgresFileRepositoryImpl(
    val repository: FileCrudRepository
) : FileRepository {

    override fun findById(id: Long): UploadedFile? {
        return repository.findById(id)
    }

    override fun saveFile(file: UploadedFile): UploadedFile {
        return repository.save(file) ?: throw DataNotSavedException()
    }

    override fun deleteFile(id: Long, publicKey: String): Long {
        val count = repository.deleteByIdAndPublicKey(id, publicKey)
        if (count > 0) {
            return id
        }

        return 0
    }

    override fun deleteByPublicKey(publicKey: String): Long = repository.deleteByPublicKey(publicKey)

    override fun findByPublicKey(publicKey: String): List<UploadedFile> {
        return repository.findByPublicKey(publicKey)
    }

    override fun findByIdAndPublicKey(id: Long, publicKey: String): UploadedFile? {
        return repository.findByIdAndPublicKey(id, publicKey)
    }
}
