package com.bitclave.node.repository.file

import com.bitclave.node.repository.models.UploadedFile

interface FileRepository {

    fun findById(id: Long): UploadedFile?

    fun saveFile(file: UploadedFile): UploadedFile

    fun deleteFile(id: Long, publicKey: String): Long

    fun deleteByPublicKey(publicKey: String): Long

    fun findByPublicKey(publicKey: String): List<UploadedFile>

    fun findByIdAndPublicKey(id: Long, publicKey: String): UploadedFile?
}
