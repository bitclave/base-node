package com.bitclave.node.controllers.v1

import com.bitclave.node.controllers.AbstractController
import com.bitclave.node.repository.models.*
import com.bitclave.node.services.errors.AccessDeniedException
import com.bitclave.node.services.v1.AccountService
import com.bitclave.node.services.v1.OfferSearchService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.concurrent.CompletableFuture

private val logger = KotlinLogging.logger {}

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
        ).exceptionally { e ->
            logger.error("Request: getResult/" + searchRequestId!!.toString() + "/" + offerSearchId!!.toString() + " raised " + e)
            throw e
        }
    }

    /**
     * Returns the OfferSearches with related Offers list of provided user.
     *
     * @return {@link List<OfferSearchResultItem>}, Http status - 200.
     *
     */
    @ApiOperation("Returns the OfferSearches with related Offers list of provided user.",
            response = OfferSearchResultItem::class, responseContainer = "List")
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Success", response = List::class)
    ])
    @RequestMapping(method = [RequestMethod.GET], value = ["/user"])
    fun getResultByOwner(
            @ApiParam("public key owner of search requests")
            @RequestParam(value = "owner", required = false)
            owner: String,

            @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
            @RequestHeader("Strategy", required = false)
            strategy: String?): CompletableFuture<List<OfferSearchResultItem>> {

        return offerSearchService.getOffersAndOfferSearchesByOwnerResult(
                getStrategyType(strategy),
                owner
        ).exceptionally { e ->
            logger.error("Request: getResultByOwner/" + owner + " raised " + e)
            throw e
        }
    }

    /**
     * Returns the dangling OfferSearches by Offers
     *
     * @return {@link List<OfferSearch>}, Http status - 200.
     *
     */
    @ApiOperation("Returns the dangling OfferSearches by Offers",
            response = OfferSearch::class, responseContainer = "List")
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Success", response = List::class)
    ])
    @RequestMapping(method = [RequestMethod.GET], value = ["/byOffer"])
    fun getDanglingOfferSearchesByOffer(

            @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
            @RequestHeader("Strategy", required = false)
            strategy: String?): CompletableFuture<List<OfferSearch>> {

        return offerSearchService.getDanglingOfferSearches(
                getStrategyType(strategy),
                true,
                false
        ).exceptionally { e ->
            logger.error("Request: getDanglingOfferSearchesByOffer raised " + e)
            throw e
        }
    }

    /**
     * Returns the dangling OfferSearches by SearchRequests
     *
     * @return {@link List<OfferSearch>}, Http status - 200.
     *
     */
    @ApiOperation("Returns the dangling OfferSearches by SearchRequests",
            response = OfferSearch::class, responseContainer = "List")
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Success", response = List::class)
    ])
    @RequestMapping(method = [RequestMethod.GET], value = ["/bySearchRequest"])
    fun getDanglingOfferSearchesBySearchRequest(

            @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
            @RequestHeader("Strategy", required = false)
            strategy: String?): CompletableFuture<List<OfferSearch>> {

        return offerSearchService.getDanglingOfferSearches(
                getStrategyType(strategy),
                false,
                true
        ).exceptionally { e ->
            logger.error("Request: getDanglingOfferSearchesBySearchRequest raised " + e)
            throw e
        }
    }

    /**
     * Returns offerSearches with the same owner and offerId but different content (status/events)
     *
     * @return {@link List<OfferSearch>}, Http status - 200.
     *
     */
    @ApiOperation("Returns offerSearches with the same owner and offerId but different content (status/events)",
            response = OfferSearch::class, responseContainer = "List")
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Success", response = List::class)
    ])
    @RequestMapping(method = [RequestMethod.GET], value = ["/conflicted"])
    fun getDiffOfferSearches(

            @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
            @RequestHeader("Strategy", required = false)
            strategy: String?): CompletableFuture<List<OfferSearch>> {

        return offerSearchService.getDiffOfferSearches(getStrategyType(strategy)).exceptionally { e ->
            logger.error("Request: getDiffOfferSearches raised " + e)
            throw e
        }
    }

    /**
     * Returns the total count of OfferSearches
     *
     * @return {@link Long}, Http status - 200.
     *
     */
    @ApiOperation("Returns the total count of OfferSearches",
            response = Long::class)
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Success", response = Long::class)
    ])
    @RequestMapping(method = [RequestMethod.GET], value = ["/count"])
    fun getOfferSearchTotalCount(

            @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
            @RequestHeader("Strategy", required = false)
            strategy: String?): CompletableFuture<Long> {

        return offerSearchService.getOfferSearchTotalCount(getStrategyType(strategy)).exceptionally { e ->
            logger.error("Request: getOfferSearchTotalCount raised " + e)
            throw e
        }
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
                        offerSearchService.saveNewOfferSearch(request.data!!, getStrategyType(strategy))
                    }.exceptionally { e ->
                        logger.error("Request: putOfferSearchItem/" + request.toString() + " raised " + e)
                        throw e
                    }
    }

    /**
     * Add event to history
     *
     */
    @ApiOperation("Add event")
    @ApiResponses(value = [ApiResponse(code = 201, message = "Added")])
    @RequestMapping(method = [RequestMethod.PATCH], value = ["/event/{id}"])
    @ResponseStatus(HttpStatus.OK)
    fun addEvent(
            @ApiParam("id of search result item")
            @PathVariable(value = "id")
            searchResultId: Long,

            @ApiParam("where client sends searchResult id and signature of the message.", required = true)
            @RequestBody
            event: SignedRequest<String>,

            @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
            @RequestHeader("Strategy", required = false)
            strategy: String?): CompletableFuture<Void> {

        return accountService.accountBySigMessage(event, getStrategyType(strategy))
                .thenCompose {
                    offerSearchService.addEventTo(event.data!!, searchResultId, getStrategyType(strategy))
                }.exceptionally { e ->
                    logger.error("Request: addEvent/" + event.toString() + " raised " + e)
                    throw e
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
                    offerSearchService.complain(request.data!!, it.publicKey, getStrategyType(strategy)).get()
                    accountService.incrementNonce(it, getStrategyType(strategy)).get()
                }.exceptionally { e ->
                    logger.error("Request: complain/" + request.toString() + " raised " + e)
                    throw e
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
                    offerSearchService.reject(request.data!!, it.publicKey, getStrategyType(strategy)).get()
                    accountService.incrementNonce(it, getStrategyType(strategy)).get()
                }.exceptionally { e ->
                    logger.error("Request: reject/" + request.toString() + " raised " + e)
                    throw e
                }
    }

    /**
     * Marks Search Result item for <evaluation>
     */
    @ApiOperation("Updates status of the selected SearchResults item to <evaluate>.")
    @ApiResponses(value = [ApiResponse(code = 200, message = "Success")])
    @RequestMapping(method = [RequestMethod.PATCH], value = ["/evaluate/{id}"])
    fun evaluate(
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
                    offerSearchService.evaluate(request.data!!, it.publicKey, getStrategyType(strategy)).get()
                    accountService.incrementNonce(it, getStrategyType(strategy)).get()
                }.exceptionally { e ->
                    logger.error("Request: evaluate/" + request.toString() + " raised " + e)
                    throw e
                }
    }

    /**
     * Marks Search Result as item that was purchased by user but not confirmed by vendor yet
     */
    @ApiOperation("Updates status of the selected SearchResults item to <claim purchase>.")
    @ApiResponses(value = [ApiResponse(code = 200, message = "Success")])
    @RequestMapping(method = [RequestMethod.PATCH], value = ["/claimpurchase/{id}"])
    fun claimPurchase(
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
                    offerSearchService.claimPurchase(request.data!!, it.publicKey, getStrategyType(strategy)).get()
                    accountService.incrementNonce(it, getStrategyType(strategy)).get()
                }.exceptionally { e ->
                    logger.error("Request: claimPurchase/" + request.toString() + " raised " + e)
                    throw e
                }
    }

    @ApiOperation("Updates status of the selected SearchResults item to <confirmed>.")
    @ApiResponses(value = [ApiResponse(code = 200, message = "Success")])
    @RequestMapping(method = [RequestMethod.PATCH], value = ["/confirm/{id}"])
    fun confirm(
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
                    offerSearchService.confirm(request.data!!, it.publicKey, getStrategyType(strategy)).get()
                    accountService.incrementNonce(it, getStrategyType(strategy)).get()
                }.exceptionally { e ->
                    logger.error("Request: confirm/" + request.toString() + " raised " + e)
                    throw e
                }

    }


    /**
     * Clone existing offer searches of a search request to another search request.
     * The API will verify that the request is cryptographically signed by the owner of the public key.
     * @param request is {@link SignedRequest} where client sends {@link SearchRequest} and
     * signature of the message.
     *
     * @return {@link List<OfferSearch>}, Http status - 200.
     *
     * @exception   {@link BadArgumentException} - 400
     *              {@link AccessDeniedException} - 403
     *              {@link NotFoundException} - 500
     */
    @ApiOperation("Clone existing offer searches of a search request to another search request.\n" +
            "The API will verify that the request is cryptographically signed by the owner of the public key.",
            response = OfferSearch::class,
            responseContainer = "List")
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Success", response = List::class)
    ])
    @RequestMapping(method = [RequestMethod.PUT], value = ["/{owner}/{id}"])
    fun cloneOfferSearchOfSearchRequest(
            @ApiParam("public key owner of target search request")
            @PathVariable(value = "owner")
            owner: String,

            @ApiParam("id of source search request.")
            @PathVariable(value = "id")
            id: Long,

            @ApiParam("where client sends SearchRequest and signature of the message.", required = true)
            @RequestBody
            request: SignedRequest<SearchRequest>,

            @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
            @RequestHeader("Strategy", required = false)
            strategy: String?): CompletableFuture<List<OfferSearch>> {

        return accountService.accountBySigMessage(request, getStrategyType(strategy))
                .thenCompose { account: Account -> accountService.validateNonce(request, account) }
                .thenCompose {
                    if (owner != it.publicKey) {
                        throw RuntimeException("Signature missmatch: content vs request  have different keys")
                    }

                    val result = offerSearchService.cloneOfferSearchOfSearchRequest(
                            id,
                            request.data!!,
                            getStrategyType(strategy)
                    ).get()

                    accountService.incrementNonce(it, getStrategyType(strategy)).get()

                    CompletableFuture.completedFuture(result)
                }.exceptionally { e ->
                    logger.error("Request: cloneOfferSearchOfSearchRequest/" + request.toString() + " raised " + e)
                    throw e
                }
    }

}
