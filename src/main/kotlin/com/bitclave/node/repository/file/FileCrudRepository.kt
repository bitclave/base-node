package com.bitclave.node.repository.file

import com.bitclave.node.repository.entities.UploadedFile
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
interface FileCrudRepository : CrudRepository<UploadedFile, String> {

    @Transactional(readOnly = true)
    fun findByPublicKey(publicKey: String): List<UploadedFile>

    @Transactional(readOnly = true)
    fun findById(id: Long): UploadedFile?

    fun deleteByIdAndPublicKey(id: Long, publicKey: String): Long

    fun deleteByPublicKey(publicKey: String): Long

    @Transactional(readOnly = true)
    fun findByIdAndPublicKey(id: Long, publicKey: String): UploadedFile?
}
