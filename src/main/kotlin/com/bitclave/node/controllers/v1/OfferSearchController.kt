package com.bitclave.node.controllers.v1

import com.bitclave.node.controllers.AbstractController
import com.bitclave.node.repository.models.OfferSearchResultItem
import com.bitclave.node.repository.models.SignedRequest
import com.bitclave.node.services.errors.AccessDeniedException
import com.bitclave.node.services.v1.AccountService
import com.bitclave.node.services.v1.OfferSearchService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.bind.annotation.*
import java.util.concurrent.CompletableFuture

@RestController
@RequestMapping("/v1/client/{clientId}/search/result")
class OfferSearchController(
        @Qualifier("v1") private val accountService: AccountService,
        @Qualifier("v1") private val offerSearchService: OfferSearchService
) : AbstractController() {

    /**
     * Returns the list of the candidates selected for the search request.
     *
     * @return {@link List<OfferSearchResultItem>}, Http status - 200.
     *
     */
    @ApiOperation("Returns the list of the candidates selected for the search request.",
            response = OfferSearchResultItem::class, responseContainer = "List")
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Success", response = List::class)
    ])
    @RequestMapping(method = [RequestMethod.GET])
    fun getResult(
            @ApiParam("public key of client")
            @PathVariable(value = "clientId")
            clientId: String,

            @ApiParam("id of search request")
            @RequestParam(value = "searchRequestId", required = false)
            searchRequestId: Long?,

            @ApiParam("id of search result item")
            @RequestParam(value = "searchResultId", required = false)
            searchResultId: Long?,

            @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
            @RequestHeader("Strategy", required = false)
            strategy: String?): CompletableFuture<List<OfferSearchResultItem>> {

        return offerSearchService.getOffersResult(
                clientId,
                getStrategyType(strategy),
                searchRequestId,
                searchResultId
        )
    }

    /**
     * Complain to Offer or search result item.
     */
    @ApiOperation("Returns the list of the candidates selected for the search request.")
    @ApiResponses(value = [ApiResponse(code = 200, message = "Success")])
    @RequestMapping(method = [RequestMethod.PATCH], value = ["/{id}"])
    fun complain(
            @ApiParam("public key of client")
            @PathVariable(value = "clientId")
            clientId: String,

            @ApiParam("id of search result item")
            @PathVariable(value = "id")
            searchResultId: Long,

            @ApiParam("where client sends searchResult id and signature of the message.", required = true)
            @RequestBody
            request: SignedRequest<Long>,

            @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
            @RequestHeader("Strategy", required = false)
            strategy: String?): CompletableFuture<Void> {

        return accountService.accountBySigMessage(request, getStrategyType(strategy))
                .thenAcceptAsync {
                    if (clientId != it.publicKey || searchResultId != request.data!!) {
                        throw AccessDeniedException()
                    }

                    offerSearchService.complain(clientId, request.data, getStrategyType(strategy)).get()
                    accountService.incrementNonce(it, getStrategyType(strategy)).get()
                }
    }

}
