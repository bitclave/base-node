package com.bitclave.node.controllers.v1

import com.bitclave.node.controllers.AbstractController
import com.bitclave.node.models.SignedRequest
import com.bitclave.node.repository.entities.Account
import com.bitclave.node.services.v1.AccountService
import com.bitclave.node.services.v1.ClientProfileService
import com.bitclave.node.services.v1.FileService
import com.bitclave.node.services.v1.OfferSearchService
import com.bitclave.node.services.v1.OfferService
import com.bitclave.node.services.v1.RequestDataService
import com.bitclave.node.services.v1.SearchRequestService
import com.bitclave.node.utils.Logger
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.CompletableFuture

@RestController
@RequestMapping("/v1/")
class AuthController(
    @Qualifier("v1") private val accountService: AccountService,
    @Qualifier("v1") private val profileService: ClientProfileService,
    @Qualifier("v1") private val requestDataService: RequestDataService,
    @Qualifier("v1") private val offerService: OfferService,
    @Qualifier("v1") private val searchRequestService: SearchRequestService,
    @Qualifier("v1") private val offerSearchService: OfferSearchService,
    @Qualifier("v1") private val fileService: FileService
) : AbstractController() {

    /**
     * Creates a new user in the system, based on the provided information.
     * The API will verify that the request is cryptographically signed by the owner of the public key.
     * @param request is {@link SignedRequest} where client sends {@link Account} and
     * signature of the message.
     *
     * @return {@link Account}, Http status - 201.
     *
     * @exception {@link BadArgumentException} - 400
     *              {@link AccessDeniedException} - 403
     *              {@link AlreadyRegisteredException} - 409
     *              {@link DataNotSaved} - 500
     */

    @ApiOperation(
        "Creates a new user in the system, based on the provided information.\n" +
            "The API will verify that the request is cryptographically signed by the owner of the public key.",
        response = Account::class
    )
    @ApiResponses(
        value = [
            ApiResponse(code = 201, message = "Created", response = Account::class),
            ApiResponse(code = 400, message = "BadArgumentException"),
            ApiResponse(code = 403, message = "AccessDeniedException"),
            ApiResponse(code = 409, message = "AlreadyRegisteredException"),
            ApiResponse(code = 500, message = "DataNotSaved")
        ]
    )
    @RequestMapping(method = [RequestMethod.POST], value = ["registration"])
    @ResponseStatus(value = HttpStatus.CREATED)
    fun registration(
        @ApiParam("where client sends Account and signature of the message.", required = true)
        @RequestBody
        request: SignedRequest<Account>,

        @ApiParam(
            "change repository strategy",
            allowableValues = "POSTGRES, HYBRID",
            required = false
        )
        @RequestHeader("Strategy", required = false)
        strategy: String?
    ): CompletableFuture<Account> {

        return accountService.checkSigMessage(request)
            .thenApply { pk ->
                if (pk != request.data?.publicKey) {
                    throw RuntimeException("Signature missmatch: content vs request  have different keys")
                }
                pk
            }
            .thenCompose {
                accountService.registrationClient(request.data!!, getStrategyType(strategy))
            }.exceptionally { e ->
                Logger.error("Request: registration/$request raised", e)
                throw e
            }
    }

    /**
     * Verifies if the specified account already exists in the system.
     * The API will verify that the request is cryptographically signed by
     * the owner of the public key.
     * @param request is {@link SignedRequest} where client sends {@link Account}
     * and signature of the message.
     *
     * @return {@link Account}, Http status - 200.
     *
     * @exception {@link AccessDeniedException} - 403
     *              {@link NotFoundException} - 404
     */
    @ApiOperation(
        "Verifies if the specified account already exists in the system..\n" +
            "The API will verify that the request is cryptographically signed by " +
            "the owner of the public key.", response = Account::class
    )
    @ApiResponses(
        value = [
            ApiResponse(code = 200, message = "Success", response = Account::class),
            ApiResponse(code = 403, message = "AccessDeniedException"),
            ApiResponse(code = 404, message = "NotFoundException")
        ]
    )
    @RequestMapping(method = [RequestMethod.POST], value = ["exist"])
    fun existAccount(
        @ApiParam("where client sends Account and signature of the message.", required = true)
        @RequestBody
        request: SignedRequest<Account>,

        @ApiParam(
            "change repository strategy",
            allowableValues = "POSTGRES, HYBRID",
            required = false
        )
        @RequestHeader("Strategy", required = false)
        strategy: String?
    ): CompletableFuture<Account> {

        return accountService.checkSigMessage(request)
            .thenApply { pk ->
                if (pk != request.data?.publicKey) {
                    Logger.debug("checkSigMessage failure $pk vs ${request.data?.publicKey}")
                    throw RuntimeException("Signature missmatch: content vs request  have different keys")
                }
                pk
            }
            .thenCompose {
                accountService.existAccount(request.data!!, getStrategyType(strategy))
            }.exceptionally { e ->
                Logger.error("Request: existAccount/$request raised", e)
                throw e
            }
    }

    /**
     * Verifies if the specified account already exists in the system.
     * The API will verify that the request is cryptographically signed by
     * the owner of the public key.
     * @param request is {@link SignedRequest} where client sends {@link Account}
     * and signature of the message.
     *
     * @return {Boolean}, true/false Http status - 200.
     *
     */
    @ApiOperation(
        "Verifies if the specified account already exists in the system..\n" +
            "The API will verify that the request is cryptographically signed by " +
            "the owner of the public key.", response = Boolean::class
    )
    @ApiResponses(
        value = [
            ApiResponse(code = 200, message = "Success", response = Boolean::class)
        ]
    )
    @RequestMapping(method = [RequestMethod.POST], value = ["existed"])
    fun isExistedAccount(
        @ApiParam("where client sends Account and signature of the message.", required = true)
        @RequestBody
        request: SignedRequest<Account>,

        @ApiParam(
            "change repository strategy",
            allowableValues = "POSTGRES, HYBRID",
            required = false
        )
        @RequestHeader("Strategy", required = false)
        strategy: String?
    ): CompletableFuture<Boolean> {

        return accountService.checkSigMessage(request)
            .thenApply { pk ->
                if (pk != request.data?.publicKey) {
                    return@thenApply false
                }

                accountService.existAccount(request.data!!, getStrategyType(strategy)).get()

                true
            }.exceptionally { false }
    }

    /**
     * Get count transactions by Account
     * @param publicKey is {@link String} where client sends {@link Account}
     * and signature of the message.
     *
     * @return {@link Long}, Http status - 200.
     *
     * @exception {@link AccessDeniedException} - 403
     *              {@link NotFoundException} - 404
     */
    @ApiOperation("Get count transactions by Account", response = Long::class)
    @ApiResponses(
        value = [
            ApiResponse(code = 200, message = "Success", response = Account::class),
            ApiResponse(code = 404, message = "NotFoundException")
        ]
    )
    @RequestMapping(method = [RequestMethod.GET], value = ["nonce/{pk}"])
    fun getNonce(
        @ApiParam("ID (Public Key) of the user in BASE system", required = true)
        @PathVariable("pk")
        publicKey: String,

        @ApiParam(
            "change repository strategy",
            allowableValues = "POSTGRES, HYBRID",
            required = false
        )
        @RequestHeader("Strategy", required = false)
        strategy: String?
    ): CompletableFuture<Long> {

        return accountService.getNonce(publicKey, getStrategyType(strategy)).exceptionally { e ->
            Logger.error("Request: getNonce/$publicKey raised", e)
            throw e
        }
    }

    /**
     * Verifies if the specified account already exists in the system.
     * The API will verify that the request is cryptographically signed by
     * the owner of the public key.
     * @param request is {@link SignedRequest} where client sends {@link Account}
     * and signature of the message.
     *
     * @return Http status - 200.
     *
     * @exception {@link AccessDeniedException} - 403
     *
     */
/**/
    @ApiOperation(
        "Delete a user from the system.\n" +
            "The API will verify that the request is cryptographically signed by the owner of the public key."
    )
    @ApiResponses(
        value = [
            ApiResponse(code = 200, message = "Success"),
            ApiResponse(code = 400, message = "BadArgumentException"),
            ApiResponse(code = 403, message = "AccessDeniedException")
        ]
    )
    @RequestMapping(method = [RequestMethod.DELETE], value = ["delete"])
    @ResponseStatus(HttpStatus.OK)
    fun deleteUser(
        @ApiParam("where client sends Account and signature of the message.", required = true)
        @RequestBody
        request: SignedRequest<Account>,

        @ApiParam(
            "change repository strategy",
            allowableValues = "POSTGRES, HYBRID",
            required = false
        )
        @RequestHeader("Strategy", required = false)
        strategy: String?
    ): CompletableFuture<Void> {
        val strategyType = getStrategyType(strategy)
        return accountService.accountBySigMessage(request, strategyType)
            .thenAcceptAsync {
                if (it.publicKey != request.pk) {
                    throw RuntimeException("Signature missmatch: content vs request  have different keys")
                }

                accountService.deleteAccount(it.publicKey, strategyType).get()
                profileService.deleteData(it.publicKey, strategyType).get()
                requestDataService.deleteRequestsAndResponses(it.publicKey, strategyType).get()
                offerService.deleteOffers(it.publicKey, strategyType).get()
                offerSearchService.deleteByOwner(it.publicKey, strategyType).get()
                searchRequestService.deleteSearchRequests(it.publicKey, strategyType).get()
                searchRequestService.deleteQuerySearchRequest(it.publicKey).get()
                fileService.deleteFileByPublicKey(it.publicKey, strategyType).get()
            }.exceptionally { e ->
                Logger.error("Request: deleteUser/$request raised", e)
                throw e
            }
    }

    /**
     * Returns the total count of Accounts
     *
     * @return {@link Long}, Http status - 200.
     *
     */
    @ApiOperation(
        "Returns the total count of Accounts",
        response = Long::class
    )
    @ApiResponses(
        value = [
            ApiResponse(code = 200, message = "Success", response = Long::class)
        ]
    )
    @RequestMapping(method = [RequestMethod.GET], value = ["account/count"])
    fun getAccountTotalCount(

        @ApiParam("change repository strategy", allowableValues = "POSTGRES", required = false)
        @RequestHeader("Strategy", required = false)
        strategy: String?
    ): CompletableFuture<Long> {
        return accountService.getAccountTotalCount(getStrategyType(strategy)).exceptionally { e ->
            Logger.error("Request: getAccountTotalCount raised", e)
            throw e
        }
    }
}
