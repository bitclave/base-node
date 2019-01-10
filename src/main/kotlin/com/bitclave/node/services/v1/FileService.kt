package com.bitclave.node.services.v1

import com.bitclave.node.extensions.validateSig
import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.file.FileRepository
import com.bitclave.node.repository.models.UploadedFile
import com.bitclave.node.repository.models.SignedRequest
import com.bitclave.node.services.errors.AccessDeniedException
import com.bitclave.node.services.errors.AlreadyRegisteredException
import com.bitclave.node.services.errors.BadArgumentException
import com.bitclave.node.services.errors.NotFoundException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.concurrent.CompletableFuture

@Service
@Qualifier("v1")
class FileService(private val fileRepository: RepositoryStrategy<FileRepository>) {

    fun getFile(id: Long, publicKey: String, strategy: RepositoryStrategyType): CompletableFuture<UploadedFile> {

        return CompletableFuture.supplyAsync({
            val repository = fileRepository.changeStrategy(strategy)

            if (id > 0 && publicKey != "0x0") {
                val file = repository.findByIdAndPublicKey(id, publicKey)

                if (file != null) {
                    return@supplyAsync file
                }
                return@supplyAsync null

            }
            return@supplyAsync null
        })
    }

    fun saveFile(
            data: MultipartFile,
            publicKey: String,
            id: Long,
            strategy: RepositoryStrategyType
    ): CompletableFuture<UploadedFile> {

        return CompletableFuture.supplyAsync {
            if (data.name.isNullOrEmpty() ) {
                throw BadArgumentException()
            }
            if (id != null && id > 0) {
                fileRepository.changeStrategy(strategy)
                        .findByIdAndPublicKey(id, publicKey) ?: throw BadArgumentException()
            }
            val file = UploadedFile(id, publicKey, data.getOriginalFilename(), data.contentType, data.size, data.bytes)

            val processedFile = fileRepository.changeStrategy(strategy).saveFile(file)
            val updatedFile = fileRepository.changeStrategy(strategy).findById(processedFile.id)
            updatedFile

        }
    }

    fun deleteFile(
            id: Long,
            publicKey: String,
            strategy: RepositoryStrategyType
    ): CompletableFuture<Long> {

        return CompletableFuture.supplyAsync({
            val deletedId = fileRepository.changeStrategy(strategy).deleteFile(id, publicKey)
            if (deletedId == 0L) {
                throw NotFoundException()
            }
            deletedId
        })
    }

}
