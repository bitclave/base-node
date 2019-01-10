package com.bitclave.node.repository.file

import com.bitclave.node.repository.models.UploadedFile
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
interface FileCrudRepository : CrudRepository<UploadedFile, String> {

    fun findByPublicKey(publicKey: String): List<UploadedFile>

    fun findById(id: Long): UploadedFile?

    fun deleteByIdAndPublicKey(id: Long, publicKey: String): Long

    fun findByIdAndPublicKey(id: Long, publicKey: String): UploadedFile?

}
