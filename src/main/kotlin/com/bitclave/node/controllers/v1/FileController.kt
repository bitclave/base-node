package com.bitclave.node.controllers.v1

import com.bitclave.node.controllers.AbstractController
import com.bitclave.node.repository.entities.Account
import com.bitclave.node.models.SignedRequest
import com.bitclave.node.repository.entities.UploadedFile
import com.bitclave.node.services.errors.BadArgumentException
import com.bitclave.node.services.errors.NotFoundException
import com.bitclave.node.services.v1.AccountService
import com.bitclave.node.services.v1.FileService
import com.google.gson.Gson
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.concurrent.CompletableFuture
import javax.annotation.Resource

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/v1/file/{owner}")
class FileController(
    @Qualifier("v1") private val accountService: AccountService,
    @Qualifier("v1") private val fileService: FileService,
    private val gson: Gson
) : AbstractController() {

    /**
     * Creates new or updates a file in the system, based on the provided information.
     * The API will verify that the request is cryptographically signed by the owner of the public key.
     *
     * @param signature is {@link SignedRequest} where client sends {@link String} and
     * signature of the message.
     *
     * @return id of created/updated file. Http status - 200/201.
     *
     * @exception {@link BadArgumentException} - 400
     *              {@link AccessDeniedException} - 403
     *              {@link DataNotSaved} - 500
     */

    @ApiOperation(
        "Creates new or updates a file in the system, based on the provided information.",
        response = Long::class
    )
    @ApiResponses(
        value = [
            ApiResponse(code = 200, message = "Updated", response = Long::class),
            ApiResponse(code = 201, message = "Created", response = Long::class),
            ApiResponse(code = 400, message = "BadArgumentException"),
            ApiResponse(code = 403, message = "AccessDeniedException"),
            ApiResponse(code = 500, message = "DataNotSaved")
        ]
    )
    @RequestMapping(method = [RequestMethod.POST], value = ["/", "{id}"])
    fun uploadFile(
        @ApiParam("where client sends MultipartFile", required = true)
        @RequestParam(value = "data", required = true)
        data: MultipartFile,

        @ApiParam("owner of file", required = true)
        @PathVariable(value = "owner", required = true)
        owner: String,

        @ApiParam("Optional id of already created a offer. Use for update offer", required = false)
        @PathVariable(value = "id", required = false)
        id: Long?,

        @ApiParam("where client sends publicKey and signature of the message.", required = true)
        @RequestParam(value = "signature", required = true)
        signature: String,

        @ApiParam("change repository strategy", allowableValues = "POSTGRES", required = false)
        @RequestHeader("Strategy", required = false)
        strategy: String?
    ): CompletableFuture<ResponseEntity<UploadedFile>> {

        val sign = SignedRequest.valueOf<String>(gson, signature)

        return accountService
            .accountBySigMessage(sign, getStrategyType(strategy))
            .thenCompose { account: Account ->
                accountService.validateNonce(sign, account)
            }
            .thenCompose {
                if (sign.pk != owner || data.isEmpty) {
                    throw BadArgumentException()
                }
                val result = fileService.saveFile(
                    data, owner, id ?: 0,
                    getStrategyType(strategy)
                ).get()
                accountService.incrementNonce(it, getStrategyType(strategy)).get()
                CompletableFuture.completedFuture(result)
            }
            .thenCompose {
                val status = if (it.id != id) HttpStatus.CREATED else HttpStatus.OK
                CompletableFuture.completedFuture(ResponseEntity<UploadedFile>(it, status))
            }.exceptionally { e ->
                logger.error("Request: uploadFile $signature raised $e")
                throw e
            }
    }

    /**
     * Delete a file from the system.
     * The API will verify that the request is cryptographically signed by the owner of the public key.
     *
     * @param request is {@link SignedRequest} where client sends {@link Long} and
     * signature of the message.
     *
     * @return {@link Long}, Http status - 200.
     *
     * @exception {@link BadArgumentException} - 400
     *              {@link AccessDeniedException} - 403
     *              {@link DataNotSaved} - 500
     */

    @ApiOperation(
        "Delete a file from the system.\n" +
            "The API will verify that the request is cryptographically signed by the owner of the public key.",
        response = Long::class
    )
    @ApiResponses(
        value = [
            ApiResponse(code = 200, message = "Deleted", response = Long::class),
            ApiResponse(code = 400, message = "BadArgumentException"),
            ApiResponse(code = 403, message = "AccessDeniedException"),
            ApiResponse(code = 404, message = "NotFoundException")
        ]
    )
    @RequestMapping(method = [RequestMethod.DELETE], value = ["{id}"])
    fun deleteFile(
        @ApiParam("owner of file", required = true)
        @PathVariable(value = "owner", required = true)
        owner: String,

        @ApiParam("id of existed file.", required = true)
        @PathVariable(value = "id", required = true)
        id: Long,

        @ApiParam("where client sends File id and signature of the message.", required = true)
        @RequestBody
        request: SignedRequest<Long>,

        @ApiParam("change repository strategy", allowableValues = "POSTGRES", required = false)
        @RequestHeader("Strategy", required = false)
        strategy: String?
    ): CompletableFuture<Long> {

        return accountService.accountBySigMessage(request, getStrategyType(strategy))
            .thenCompose { account: Account -> accountService.validateNonce(request, account) }
            .thenCompose {
                if (request.pk != owner || id != request.data) {
                    throw BadArgumentException()
                }
                val result = fileService.deleteFile(
                    id,
                    owner,
                    getStrategyType(strategy)
                ).get()
                accountService.incrementNonce(it, getStrategyType(strategy)).get()

                CompletableFuture.completedFuture(result)
            }.exceptionally { e ->
                logger.error("Request: $request raised $e")
                throw e
            }
    }

    /**
     * Download a file in the system.
     *
     * @return {@link Resource}, Http status - 200.
     */
    @ApiOperation(
        "download a file in the system", response = Resource::class
    )
    @ApiResponses(
        value = [
            ApiResponse(code = 200, message = "Success", response = Resource::class),
            ApiResponse(code = 404, message = "NotFoundException")
        ]
    )
    @RequestMapping(method = [RequestMethod.GET], value = ["{id}"])
    fun downloadFile(
        @ApiParam("owner who create file", required = true)
        @PathVariable("owner", required = true)
        owner: String,

        @ApiParam("id of already created file.", required = true)
        @PathVariable(value = "id", required = true)
        id: Long,

        @ApiParam("change repository strategy", allowableValues = "POSTGRES", required = false)
        @RequestHeader("Strategy", required = false)
        strategy: String?
    ): CompletableFuture<ResponseEntity<ByteArray>> {

        return fileService.getFile(id, owner, getStrategyType(strategy))
            .thenCompose {
                if (it == null || it.id != id || it.data == null) throw NotFoundException()
                CompletableFuture.completedFuture(
                    ResponseEntity.ok()
                        .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + it.name + "\""
                        )
                        .body(it.data)
                )
            }.exceptionally { e ->
                logger.error("Request: downloadFile $owner $id raised $e")
                throw e
            }
    }
}
