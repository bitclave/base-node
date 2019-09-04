package com.bitclave.node.controllers.v2

import com.bitclave.node.controllers.AbstractController
import com.bitclave.node.repository.entities.Account
import com.bitclave.node.repository.entities.SearchRequest
import com.bitclave.node.models.SignedRequest
import com.bitclave.node.services.v1.AccountService
import com.bitclave.node.services.v1.SearchRequestService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import mu.KotlinLogging
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
@RequestMapping("/v2/client/{owner}/search/request")
class SearchRequestControllerV2(
    @Qualifier("v1") private val accountService: AccountService,
    @Qualifier("v1") private val searchRequestService: SearchRequestService
) : AbstractController() {
    private val logger = KotlinLogging.logger {}

    /**
     * Creates new or updates requests for search in the system, based on the provided information.
     * The API will verify that the request is cryptographically signed by the owner of the public key.
     * @param request is {@link SignedRequest} where client sends {@link SearchRequest} and
     * signature of the message.
     *
     * @return {@link SearchRequest}, Http status - 200.
     *
     * @exception {@link BadArgumentException} - 400
     *              {@link AccessDeniedException} - 403
     *              {@link DataNotSaved} - 500
     */
    @ApiOperation(
        "Creates new or updates requests for search in the system, based on the provided information.\n" +
            "The API will verify that the request is cryptographically signed by the owner of the public key.",
        response = SearchRequest::class
    )
    @ApiResponses(
        value = [
            ApiResponse(code = 200, message = "Success", response = SearchRequest::class),
            ApiResponse(code = 400, message = "BadArgumentException"),
            ApiResponse(code = 403, message = "AccessDeniedException"),
            ApiResponse(code = 500, message = "DataNotSaved")
        ]
    )
    @RequestMapping(method = [RequestMethod.POST])
    fun putSearchRequests(
        @ApiParam("public key owner of search request")
        @PathVariable(value = "owner")
        owner: String,

        @ApiParam("where client sends SearchRequests and signature of the message.", required = true)
        @RequestBody
        request: SignedRequest<List<SearchRequest>>,

        @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
        @RequestHeader("Strategy", required = false)
        strategy: String?
    ): CompletableFuture<List<SearchRequest>> {

        return accountService.accountBySigMessage(request, getStrategyType(strategy))
            .thenCompose { account: Account -> accountService.validateNonce(request, account) }
            .thenCompose {
                if (owner != it.publicKey) {
                    throw RuntimeException("Signature missmatch: content vs request  have different keys")
                }

                val result = searchRequestService.putSearchRequests(
                    it.publicKey,
                    request.data!!,
                    getStrategyType(strategy)
                ).get()

                accountService.incrementNonce(it, getStrategyType(strategy)).get()

                CompletableFuture.completedFuture(result)
            }.exceptionally { e ->
                logger.error("Request: putSearchRequests:$request raised $e")
                throw e
            }
    }

    /**
     * Clone new request with OfferSearch objects, based on the provided request.
     * The API will verify that the request is cryptographically signed by the owner of the public key.
     * @param request is {@link SignedRequest} where client sends {@link SearchRequest} and
     * signature of the message.
     *
     * @return {@link Long}, Http status - 201.
     *
     * @exception {@link BadArgumentException} - 400
     *              {@link AccessDeniedException} - 403
     *              {@link NotFoundException} - 500
     */
    @ApiOperation(
        "Clone new request with OfferSearch objects, based on the provided request.\n" +
            "The API will verify that the request is cryptographically signed by the owner of the public key.",
        response = SearchRequest::class
    )
    @ApiResponses(
        value = [
            ApiResponse(code = 201, message = "Created", response = SearchRequest::class),
            ApiResponse(code = 400, message = "BadArgumentException"),
            ApiResponse(code = 403, message = "AccessDeniedException"),
            ApiResponse(code = 500, message = "NotFoundException")
        ]
    )
    @RequestMapping(method = [RequestMethod.PUT])
    @ResponseStatus(value = HttpStatus.CREATED)
    fun cloneSearchRequest(
        @ApiParam("public key owner of search request")
        @PathVariable(value = "owner")
        owner: String,

        @ApiParam("where client sends List of SearchRequestIds and signature of the message.", required = true)
        @RequestBody
        request: SignedRequest<List<Long>>,

        @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
        @RequestHeader("Strategy", required = false)
        strategy: String?
    ): CompletableFuture<List<SearchRequest>> {

        return accountService.accountBySigMessage(request, getStrategyType(strategy))
            .thenCompose { account: Account -> accountService.validateNonce(request, account) }
            .thenCompose {
                if (owner != it.publicKey) {
                    throw RuntimeException("Signature missmatch: content vs request  have different keys")
                }

                val result = searchRequestService.cloneSearchRequestWithOfferSearches(
                    it.publicKey,
                    request.data!!,
                    getStrategyType(strategy)
                ).get()

                accountService.incrementNonce(it, getStrategyType(strategy)).get()

                CompletableFuture.completedFuture(result)
            }.exceptionally { e ->
                logger.error("Request: cloneSearchRequest/$owner/$request raised $e")
                throw e
            }
    }
}
