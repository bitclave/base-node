package com.bitclave.node.services.v1

import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.file.FileRepository
import com.bitclave.node.repository.models.UploadedFile
import com.bitclave.node.services.errors.BadArgumentException
import com.bitclave.node.services.errors.DataNotSavedException
import com.bitclave.node.services.errors.NotFoundException
import com.bitclave.node.utils.supplyAsyncEx
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.Date
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier

@Service
@Qualifier("v1")
class FileService(private val fileRepository: RepositoryStrategy<FileRepository>) {

    fun getFile(id: Long, publicKey: String, strategy: RepositoryStrategyType): CompletableFuture<UploadedFile> {
        return supplyAsyncEx(Supplier {
            val repository = fileRepository.changeStrategy(strategy)
            repository.findByIdAndPublicKey(id, publicKey) ?: throw NotFoundException("file not found")
        })
    }

    fun saveFile(
        data: MultipartFile,
        publicKey: String,
        id: Long,
        strategy: RepositoryStrategyType
    ): CompletableFuture<UploadedFile> {

        return supplyAsyncEx(Supplier {
            if (data.name.isNullOrEmpty()) {
                throw BadArgumentException()
            }
            var existedFile: UploadedFile? = null

            if (id > 0) {
                existedFile = fileRepository.changeStrategy(strategy)
                    .findByIdAndPublicKey(id, publicKey) ?: throw BadArgumentException()
            }

            val createdAt = existedFile?.createdAt ?: Date()

            val file = UploadedFile(
                id,
                publicKey,
                data.originalFilename,
                data.contentType,
                data.size,
                data.bytes,
                createdAt,
                Date()
            )

            val processedFile = fileRepository.changeStrategy(strategy).saveFile(file)

            fileRepository.changeStrategy(strategy)
                .findById(processedFile.id) ?: throw DataNotSavedException()
        })
    }

    fun deleteFile(
        id: Long,
        publicKey: String,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Long> {

        return supplyAsyncEx(Supplier {
            val deletedId = fileRepository.changeStrategy(strategy).deleteFile(id, publicKey)
            if (deletedId == 0L) {
                throw NotFoundException()
            }
            deletedId
        })
    }
}
