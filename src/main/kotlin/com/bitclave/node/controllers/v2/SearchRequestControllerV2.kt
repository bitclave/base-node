package com.bitclave.node.controllers.v2

import com.bitclave.node.controllers.AbstractController
import com.bitclave.node.repository.models.Account
import com.bitclave.node.repository.models.SearchRequest
import com.bitclave.node.repository.models.SignedRequest
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

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/v2/client/{owner}/search/request/")
class SearchRequestControllerV2(
    @Qualifier("v1") private val accountService: AccountService,
    @Qualifier("v1") private val searchRequestService: SearchRequestService
) : AbstractController() {

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

        @ApiParam("where client sends SearchRequest and signature of the message.", required = true)
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
