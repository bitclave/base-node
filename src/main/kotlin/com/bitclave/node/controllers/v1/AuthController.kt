package com.bitclave.node.controllers.v1

import com.bitclave.node.controllers.AbstractController
import com.bitclave.node.repository.models.Account
import com.bitclave.node.repository.models.SignedRequest
import com.bitclave.node.services.errors.AccessDeniedException
import com.bitclave.node.services.v1.AccountService
import com.bitclave.node.services.v1.ClientProfileService
import com.bitclave.node.services.v1.OfferService
import com.bitclave.node.services.v1.RequestDataService
import com.bitclave.node.services.v1.SearchRequestService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.concurrent.CompletableFuture

@RestController()
@RequestMapping("/v1/")
class AuthController(
        @Qualifier("v1") private val accountService: AccountService,
        @Qualifier("v1") private val profileService: ClientProfileService,
        @Qualifier("v1") private val requestDataService: RequestDataService,
        @Qualifier("v1") private val offerService: OfferService,
        @Qualifier("v1") private val searchRequestService: SearchRequestService
) : AbstractController() {

    /**
     * Creates a new user in the system, based on the provided information.
     * The API will verify that the request is cryptographically signed by the owner of the public key.
     * @param request is {@link SignedRequest} where client sends {@link Account} and
     * signature of the message.
     *
     * @return {@link Account}, Http status - 201.
     *
     * @exception   {@link BadArgumentException} - 400
     *              {@link AccessDeniedException} - 403
     *              {@link AlreadyRegisteredException} - 409
     *              {@link DataNotSaved} - 500
     */

    @ApiOperation("Creates a new user in the system, based on the provided information.\n" +
            "The API will verify that the request is cryptographically signed by the owner of the public key.",
            response = Account::class
    )
    @ApiResponses(value = [
        ApiResponse(code = 201, message = "Created", response = Account::class),
        ApiResponse(code = 400, message = "BadArgumentException"),
        ApiResponse(code = 403, message = "AccessDeniedException"),
        ApiResponse(code = 409, message = "AlreadyRegisteredException"),
        ApiResponse(code = 500, message = "DataNotSaved")
    ])
    @RequestMapping(method = [RequestMethod.POST], value = ["registration"])
    @ResponseStatus(value = HttpStatus.CREATED)
    fun registration(
            @ApiParam("where client sends Account and signature of the message.", required = true)
            @RequestBody
            request: SignedRequest<Account>,

            @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
            @RequestHeader("Strategy", required = false)
            strategy: String?
    ): CompletableFuture<Account> {

        return accountService.checkSigMessage(request)
                .thenApply { pk ->
                    if (pk != request.data?.publicKey) {
                        throw AccessDeniedException()
                    }
                    pk
                }
                .thenCompose {
                    accountService.registrationClient(request.data!!, getStrategyType(strategy))
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
     * @exception   {@link AccessDeniedException} - 403
     *              {@link NotFoundException} - 404
     */
    @ApiOperation("Verifies if the specified account already exists in the system..\n" +
            "The API will verify that the request is cryptographically signed by " +
            "the owner of the public key.", response = Account::class)
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Success", response = Account::class),
        ApiResponse(code = 403, message = "AccessDeniedException"),
        ApiResponse(code = 404, message = "NotFoundException")
    ])
    @RequestMapping(method = [RequestMethod.POST], value = ["exist"])
    fun existAccount(
            @ApiParam("where client sends Account and signature of the message.", required = true)
            @RequestBody
            request: SignedRequest<Account>,

            @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
            @RequestHeader("Strategy", required = false)
            strategy: String?
    ): CompletableFuture<Account> {

        return accountService.checkSigMessage(request)
                .thenApply { pk ->
                    if (pk != request.data?.publicKey) {
                        throw AccessDeniedException()
                    }
                    pk
                }
                .thenCompose {
                    accountService.existAccount(request.data!!, getStrategyType(strategy))
                }
    }

    /**
     * Get count transactions by Account
     * @param request is {@link SignedRequest} where client sends {@link Account}
     * and signature of the message.
     *
     * @return {@link Long}, Http status - 200.
     *
     * @exception   {@link AccessDeniedException} - 403
     *              {@link NotFoundException} - 404
     */
    @ApiOperation("Get count transactions by Account", response = Long::class)
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Success", response = Account::class),
        ApiResponse(code = 404, message = "NotFoundException")
    ])
    @RequestMapping(method = [RequestMethod.GET], value = ["nonce/{pk}"])
    fun getNonce(
            @ApiParam("ID (Public Key) of the user in BASE system", required = true)
            @PathVariable("pk")
            publicKey: String,

            @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
            @RequestHeader("Strategy", required = false)
            strategy: String?
    ): CompletableFuture<Long> {

        return accountService.getNonce(publicKey, getStrategyType(strategy))
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
     * @exception   {@link AccessDeniedException} - 403
     *
     */
/**/
    @ApiOperation("Delete a user from the system.\n" +
            "The API will verify that the request is cryptographically signed by the owner of the public key.")
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Success"),
        ApiResponse(code = 400, message = "BadArgumentException"),
        ApiResponse(code = 403, message = "AccessDeniedException")
    ])
    @RequestMapping(method = [RequestMethod.DELETE], value = ["delete"])
    @ResponseStatus(HttpStatus.OK)
    fun deleteUser(
            @ApiParam("where client sends Account and signature of the message.", required = true)
            @RequestBody
            request: SignedRequest<Account>,

            @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
            @RequestHeader("Strategy", required = false)
            strategy: String?
    ): CompletableFuture<Void> {

        val strategyType = getStrategyType(strategy)
        return accountService.accountBySigMessage(request, strategyType)
                .thenAcceptAsync {
                    if (it.publicKey != request.pk) {
                        throw AccessDeniedException()
                    }

                    accountService.deleteAccount(it.publicKey, strategyType).get()
                    profileService.deleteData(it.publicKey, strategyType).get()
                    requestDataService.deleteRequestsAndResponses(it.publicKey, strategyType).get()
                    offerService.deleteOffers(it.publicKey, strategyType).get()
                    searchRequestService.deleteSearchRequests(it.publicKey, strategyType).get()
                }
    }

}
