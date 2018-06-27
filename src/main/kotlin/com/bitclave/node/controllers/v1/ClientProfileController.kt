package com.bitclave.node.controllers.v1

import com.bitclave.node.controllers.AbstractController
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.models.Account
import com.bitclave.node.repository.models.SignedRequest
import com.bitclave.node.services.v1.AccountService
import com.bitclave.node.services.v1.ClientProfileService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.bind.annotation.*
import java.util.concurrent.CompletableFuture

@RestController
@RequestMapping("/v1/client/")
class ClientProfileController(
        @Qualifier("v1") private val accountService: AccountService,
        @Qualifier("v1") private val profileService: ClientProfileService
) : AbstractController() {

    /**
     * Returns encrypted data of the user that is identified by the given ID (Public Key).
     * @param publicKey ID (Public Key) of the user in BASE system.
     *
     * @return Map<String, String>. if client not found then empty Map is returned.
     * Http status - 200.
     */
    @ApiOperation("Returns encrypted data of the user that is identified by the given ID (Public Key).",
            response = Map::class)
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Success", response = Map::class)
    ])
    @RequestMapping(method = [RequestMethod.GET], value = ["{pk}/"])
    fun getData(
            @ApiParam("ID (Public Key) of the user in BASE system", required = true)
            @PathVariable("pk")
            publicKey: String,
            @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
            @RequestHeader("Strategy", required = false)
            strategy: RepositoryStrategyType
    ): CompletableFuture<Map<String, String>> {

        return profileService.getData(publicKey, strategy)
    }

    /**
     * Stores user’s personal data in BASE. Note, the data shall be encrypted by
     * the user before it is passed to this API. The API will verify that
     * the request is cryptographically signed by the owner of the public key.
     * @param request {@link SignedRequest} with {@link Map<String, String>} and signature of
     * the message. Map is <key, value> structure, where key and value are strings.
     * Note: “value” field shall be encrypted by the user before sending to this API.
     *
     * @return {@link Map<String,String>} same data from argument. Http status - 200.
     *
     * @exception   {@link BadArgumentException} - 400
     *              {@link AccessDeniedException} - 403
     *              {@link NotFoundException} - 404
     *              {@link DataNotSaved} - 500
     */
    @ApiOperation("Stores user’s personal data in BASE. Note, the data shall be encrypted by\n" +
            "the user before it is passed to this API. The API will verify that\n" +
            "the request is cryptographically signed by the owner of the public key.",
            response = Map::class)
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Success", response = Map::class),
        ApiResponse(code = 400, message = "BadArgumentException"),
        ApiResponse(code = 403, message = "AccessDeniedException"),
        ApiResponse(code = 404, message = "NotFoundException"),
        ApiResponse(code = 500, message = "DataNotSaved")
    ])
    @RequestMapping(method = [RequestMethod.PATCH])
    fun updateData(
            @ApiParam("SignedRequest<Map<String, String>> where Map is <key, value> structure," +
                    " where key and value are strings.", required = true)
            @RequestBody
            request: SignedRequest<Map<String, String>>,
            @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
            @RequestHeader("Strategy", required = false)
            strategy: String?
    ): CompletableFuture<Map<String, String>> {

        return accountService.accountBySigMessage(request, getStrategyType(strategy))
                .thenCompose { account: Account -> accountService.validateNonce(request, account) }
                .thenCompose { account: Account ->
                    val result = profileService.updateData(
                            account.publicKey,
                            request.data!!,
                            getStrategyType(strategy)
                    ).get()
                    accountService.incrementNonce(account, getStrategyType(strategy)).get()

                    CompletableFuture.completedFuture(result)
                }
    }

}
