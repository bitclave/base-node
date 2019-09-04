package com.bitclave.node.controllers.v2

import com.bitclave.node.controllers.AbstractController
import com.bitclave.node.repository.entities.Account
import com.bitclave.node.repository.entities.OfferSearch
import com.bitclave.node.models.SignedRequest
import com.bitclave.node.services.v1.AccountService
import com.bitclave.node.services.v1.OfferSearchService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.CompletableFuture

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/v2/search")
class OfferSearchControllerV2(
    @Qualifier("v1") private val accountService: AccountService,
    @Qualifier("v1") private val offerSearchService: OfferSearchService
) : AbstractController() {

    /**
     * Clone existing offer searches of a search request to another search request.
     * The API will verify that the request is cryptographically signed by the owner of the public key.
     * @param request is {@link SignedRequest} where client sends {@link SearchRequest} and
     * signature of the message.
     *
     * @return {@link List<OfferSearch>}, Http status - 200.
     *
     * @exception {@link BadArgumentException} - 400
     *              {@link AccessDeniedException} - 403
     *              {@link NotFoundException} - 404
     */
    @ApiOperation(
        "Clone existing offer searches of a search request to another search request.\n" +
            "The API will verify that the request is cryptographically signed by the owner of the public key.",
        response = OfferSearch::class,
        responseContainer = "List"
    )
    @ApiResponses(
        value = [
            ApiResponse(code = 200, message = "Success", response = List::class)
        ]
    )
    @RequestMapping(method = [RequestMethod.PUT], value = ["/result/{owner}"])
    fun cloneOfferSearchOfSearchRequest(
        @ApiParam("public key owner of target search request")
        @PathVariable(value = "owner")
        owner: String,

        @ApiParam(
            "where client sends List of Pair of searchRequestIds original->copyTo and signature of the message.",
            required = true
        )
        @RequestBody
        request: SignedRequest<List<Pair<Long, Long>>>,

        @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
        @RequestHeader("Strategy", required = false)
        strategy: String?
    ): CompletableFuture<List<OfferSearch>> {

        return accountService.accountBySigMessage(request, getStrategyType(strategy))
            .thenCompose { account: Account -> accountService.validateNonce(request, account) }
            .thenCompose {
                if (owner != it.publicKey) {
                    throw RuntimeException("Signature missmatch: content vs request  have different keys")
                }

                val result = offerSearchService.cloneOfferSearchOfSearchRequest(
                    owner,
                    request.data!!,
                    getStrategyType(strategy)
                ).get()

                accountService.incrementNonce(it, getStrategyType(strategy)).get()

                CompletableFuture.completedFuture(result)
            }.exceptionally { e ->
                logger.error("Request: cloneOfferSearchOfSearchRequest/$request raised $e")
                throw e
            }
    }
}
