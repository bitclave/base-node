package com.bitclave.node.controllers.v1

import com.bitclave.node.controllers.AbstractController
import com.bitclave.node.repository.models.OfferSearch
import com.bitclave.node.repository.models.OfferSearchResultItem
import com.bitclave.node.repository.models.SignedRequest
import com.bitclave.node.services.v1.AccountService
import com.bitclave.node.services.v1.OfferSearchService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.concurrent.CompletableFuture

@RestController
@RequestMapping("/v1/search/result")
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
            @ApiParam("id of search request")
            @RequestParam(value = "searchRequestId", required = false)
            searchRequestId: Long?,

            @ApiParam("id of search result item")
            @RequestParam(value = "offerSearchId", required = false)
            offerSearchId: Long?,

            @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
            @RequestHeader("Strategy", required = false)
            strategy: String?): CompletableFuture<List<OfferSearchResultItem>> {

        return offerSearchService.getOffersResult(
                getStrategyType(strategy),
                searchRequestId,
                offerSearchId
        )
    }

    /**
     * Add offer to search result for some search request
     *
     */
    @ApiOperation("Add offer to search result for some search request")
    @ApiResponses(value = [ApiResponse(code = 201, message = "Created")])
    @RequestMapping(method = [RequestMethod.POST])
    @ResponseStatus(HttpStatus.CREATED)
    fun putOfferSearchItem(
            @ApiParam("where client sends offer search model and signature of the message.")
            @RequestBody
            request: SignedRequest<OfferSearch>,

            @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
            @RequestHeader("Strategy", required = false)
            strategy: String?): CompletableFuture<Void> {

        return accountService.accountBySigMessage(request, getStrategyType(strategy))
                .thenCompose {
                    offerSearchService.saveOfferSearch(request.data!!, getStrategyType(strategy))
                }
    }

    /**
     * Complain to Offer or search result item.
     */
    @ApiOperation("[Legacy] Updates status of the selected SearchResults item to <reject>.")
    @ApiResponses(value = [ApiResponse(code = 200, message = "Success")])
    @RequestMapping(method = [RequestMethod.PATCH], value = ["/{id}"])
    fun complain(
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
                    offerSearchService.complain(request.data!!, getStrategyType(strategy)).get()
                    accountService.incrementNonce(it, getStrategyType(strategy)).get()
                }
    }

    /**
     * Reject Search Result
     */
    @ApiOperation("Updates status of the selected SearchResults item to <reject>.")
    @ApiResponses(value = [ApiResponse(code = 200, message = "Success")])
    @RequestMapping(method = [RequestMethod.PATCH], value = ["/reject/{id}"])
    fun reject(
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
                    offerSearchService.reject(request.data!!, getStrategyType(strategy)).get()
                    accountService.incrementNonce(it, getStrategyType(strategy)).get()
                }
    }

    /**
     * Marks Search Result item for <evaluation>
     */
    @ApiOperation("Updates status of the selected SearchResults item to <evalaute>.")
    @ApiResponses(value = [ApiResponse(code = 200, message = "Success")])
    @RequestMapping(method = [RequestMethod.PATCH], value = ["/evaluate/{id}"])
    fun evalaute(
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
                    offerSearchService.evaluate(request.data!!, getStrategyType(strategy)).get()
                    accountService.incrementNonce(it, getStrategyType(strategy)).get()
                }
    }


}
