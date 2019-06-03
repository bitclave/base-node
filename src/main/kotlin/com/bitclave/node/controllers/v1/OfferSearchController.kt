package com.bitclave.node.controllers.v1

import com.bitclave.node.controllers.AbstractController
import com.bitclave.node.repository.models.Account
import com.bitclave.node.repository.models.OfferResultAction
import com.bitclave.node.repository.models.OfferSearch
import com.bitclave.node.repository.models.OfferSearchResultItem
import com.bitclave.node.repository.models.SearchRequest
import com.bitclave.node.repository.models.SignedRequest
import com.bitclave.node.services.v1.AccountService
import com.bitclave.node.services.v1.OfferSearchService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.net.URLDecoder
import java.util.concurrent.CompletableFuture

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/v1/search")
class OfferSearchController(
    @Qualifier("v1") private val accountService: AccountService,
    @Qualifier("v1") private val offerSearchService: OfferSearchService
) : AbstractController() {

    /**
     * Creates new offerSearches, based on the query full text search.
     * The API will verify that the request is cryptographically signed by the owner of the public key.
     * @param request is {@link SignedRequest} where client sends {@link SearchRequest} and
     * signature of the message.
     *
     * @return {@link List<OfferSearchResultItem>}, Http status - 201.
     *
     * @exception {@link BadArgumentException} - 400
     *              {@link AccessDeniedException} - 403
     *              {@link DataNotSaved} - 500
     */
    @ApiOperation(
        "Creates new offerSearches, based on the the query full text search.\n" +
            "The API will verify that the request is cryptographically signed by the owner of the public key.",
        response = List::class
    )
    @ApiResponses(
        value = [
            ApiResponse(code = 201, message = "Created", response = List::class),
            ApiResponse(code = 400, message = "BadArgumentException"),
            ApiResponse(code = 403, message = "AccessDeniedException"),
            ApiResponse(code = 404, message = "NotFoundException"),
            ApiResponse(code = 500, message = "DataNotSaved")
        ]
    )
    @RequestMapping(method = [RequestMethod.POST], value = ["/query"])
    @ResponseStatus(HttpStatus.CREATED)
    fun createOfferSearchesByQuery(
        @ApiParam("sends full text search query string")
        @RequestParam(value = "q")
        query: String,

        @ApiParam("Optional page number to retrieve a particular page. If not specified this API retrieves first page.")
        @RequestParam("page", defaultValue = "0", required = false)
        page: Int,

        @ApiParam("Optional page size to include number of offerSearch items in a page. Defaults to 20.")
        @RequestParam("size", defaultValue = "20", required = false)
        size: Int,

        @ApiParam("sends the list of the interests e.g. 'interest_bitclave_general'")
        @RequestParam(value = "interests", required = false)
        interests: List<String>,

        @ApiParam("mode of interests list must or prefer")
        @RequestParam(value = "mode", required = false)
        mode: String,

        @ApiParam(
            "where client already existed search request id (who has rtSearch tag)" +
                " and signature of the message.", required = true
        )
        @RequestBody
        request: SignedRequest<Long>,

        @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
        @RequestHeader("Strategy", required = false)
        strategy: String?
    ): CompletableFuture<Page<OfferSearchResultItem>> {

        return accountService.accountBySigMessage(request, getStrategyType(strategy))
            .thenCompose { account: Account -> accountService.validateNonce(request, account) }
            .thenCompose {
                val decodedQuery = URLDecoder.decode(query, "UTF-8")
                val result = offerSearchService.createOfferSearchesByQuery(
                    request.data!!,
                    it.publicKey,
                    decodedQuery,
                    PageRequest(page, size),
                    getStrategyType(strategy),
                    interests,
                    mode
                ).get()

                accountService.incrementNonce(it, getStrategyType(strategy)).get()

                CompletableFuture.completedFuture(result)
            }.exceptionally { e ->
                logger.error("Request: createOfferSearchesByQuery -> request: $request; error:$e")
                throw e
            }
    }

    /**
     * Returns the list of the candidates selected for the search request.
     *
     * @return {@link List<OfferSearchResultItem>}, Http status - 200.
     *
     */
    @ApiOperation(
        "Returns the list of the candidates selected for the search request.",
        response = OfferSearchResultItem::class, responseContainer = "List"
    )
    @ApiResponses(
        value = [
            ApiResponse(code = 200, message = "Success", response = List::class)
        ]
    )
    @RequestMapping(method = [RequestMethod.GET], value = ["/result"])
    fun getResult(
        @ApiParam("id of search request")
        @RequestParam(value = "searchRequestId", required = false, defaultValue = "0")
        searchRequestId: Long,

        @ApiParam("id of search result item")
        @RequestParam(value = "offerSearchId", required = false, defaultValue = "0")
        offerSearchId: Long,

        @ApiParam("Optional page number to retrieve a particular page. If not specified this API retrieves first page.")
        @RequestParam("page", defaultValue = "0", required = false)
        page: Int,

        @ApiParam("Optional page size to include number of offerSearchResult items in a page. Defaults to 20.")
        @RequestParam("size", defaultValue = "20", required = false)
        size: Int,

        @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
        @RequestHeader("Strategy", required = false)
        strategy: String?
    ): CompletableFuture<Page<OfferSearchResultItem>> {

        return offerSearchService.getOffersResult(
            getStrategyType(strategy),
            searchRequestId,
            offerSearchId,
            PageRequest(page, size)
        ).exceptionally { e ->
            logger.error("Request: getResult /$searchRequestId/$offerSearchId raised $e")
            throw e
        }
    }

    /**
     * Returns the OfferSearches with related Offers list of provided user.
     *
     * @return {@link List<OfferSearchResultItem>}, Http status - 200.
     *
     */
    @ApiOperation(
        "Returns the OfferSearches with related Offers list of provided user.",
        response = OfferSearchResultItem::class, responseContainer = "List"
    )
    @ApiResponses(
        value = [
            ApiResponse(code = 200, message = "Success", response = List::class)
        ]
    )
    @RequestMapping(method = [RequestMethod.GET], value = ["/result/user"])
    fun getResultByOwner(
        @ApiParam("public key owner of search requests")
        @RequestParam(value = "owner", required = false)
        owner: String,

        @ApiParam("return unique by offerId")
        @RequestParam(value = "unique", required = false, defaultValue = "0")
        unique: Boolean,

        @ApiParam("query only in searchIds")
        @RequestParam(value = "searchIds", required = false, defaultValue = "")
        searchIds: List<Long>,

        @ApiParam("query by state")
        @RequestParam(value = "state", required = false, defaultValue = "")
        state: List<OfferResultAction>,

        @ApiParam("Optional page number to retrieve a particular page. If not specified this API retrieves first page.")
        @RequestParam("page", defaultValue = "0", required = false)
        page: Int,

        @ApiParam("Optional page size to include number of offerSearchResult items in a page. Defaults to 20.")
        @RequestParam("size", defaultValue = "20", required = false)
        size: Int,

        @ApiParam("Optional type of sorting. Default is rank.")
        @RequestParam("sort", defaultValue = "rank", required = false)
        sort: String,

        @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
        @RequestHeader("Strategy", required = false)
        strategy: String?
    ): CompletableFuture<Page<OfferSearchResultItem>> {

        return offerSearchService.getOffersAndOfferSearchesByParams(
            getStrategyType(strategy),
            owner,
            unique,
            searchIds,
            state,
            PageRequest(page, size, Sort(sort))
        ).exceptionally { e ->
            logger.error("Request: getResultByOwner/$owner raised $e")
            throw e
        }
    }

    /**
     * Returns the total count of OfferSearches
     *
     * @return {@link Long}, Http status - 200.
     *
     */
    @ApiOperation(
        "Returns the total count of OfferSearches",
        response = Long::class
    )
    @ApiResponses(
        value = [
            ApiResponse(code = 200, message = "Success", response = Long::class)
        ]
    )
    @RequestMapping(method = [RequestMethod.GET], value = ["/result/count"])
    fun getOfferSearchTotalCount(
        @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
        @RequestHeader("Strategy", required = false)
        strategy: String?
    ): CompletableFuture<Long> {

        return offerSearchService.getOfferSearchTotalCount(getStrategyType(strategy)).exceptionally { e ->
            logger.error("Request: getOfferSearchTotalCount raised $e")
            throw e
        }
    }

    /**
     * Returns the total count of OfferSearches for each search request id
     *
     * @return {@link Map<Long, Long>} where Map<SearchRequestId,count>, Http status - 200.
     *
     */
    @ApiOperation(
        "Returns the total count of OfferSearches for each search request id",
        response = Long::class
    )
    @ApiResponses(
        value = [
            ApiResponse(code = 200, message = "Success", response = Long::class)
        ]
    )
    @RequestMapping(method = [RequestMethod.GET], value = ["/count"])
    fun getOfferSearchCountBySearchRequest(
        @ApiParam("search request ids list")
        @RequestParam("ids", required = true, defaultValue = "")
        searchRequestIds: List<Long>,

        @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
        @RequestHeader("Strategy", required = false)
        strategy: String?
    ): CompletableFuture<Map<Long, Long>> {

        return offerSearchService.getOfferSearchCountBySearchRequestIds(
            searchRequestIds,
            getStrategyType(strategy)
        ).exceptionally { e ->
            logger.error("Request: getOfferSearchCountBySearchRequest raised $e")
            throw e
        }
    }

    /**
     * Add offer to search result for some search request
     *
     */
    @ApiOperation("Add offer to search result for some search request")
    @ApiResponses(value = [ApiResponse(code = 201, message = "Created")])
    @RequestMapping(method = [RequestMethod.POST], value = ["/result"])
    @ResponseStatus(HttpStatus.CREATED)
    fun putOfferSearchItem(
        @ApiParam("where client sends offer search model and signature of the message.")
        @RequestBody
        request: SignedRequest<OfferSearch>,

        @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
        @RequestHeader("Strategy", required = false)
        strategy: String?
    ): CompletableFuture<Void> {

        return accountService.accountBySigMessage(request, getStrategyType(strategy))
            .thenCompose {
                offerSearchService.saveNewOfferSearch(request.data!!, getStrategyType(strategy))
            }.exceptionally { e ->
                logger.error("Request: putOfferSearchItem/$request raised $e")
                throw e
            }
    }

    /**
     * Add event to history
     *
     */
    @ApiOperation("Add event")
    @ApiResponses(value = [ApiResponse(code = 201, message = "Added")])
    @RequestMapping(method = [RequestMethod.PATCH], value = ["/result/event/{id}"])
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
        strategy: String?
    ): CompletableFuture<Void> {

        return accountService.accountBySigMessage(event, getStrategyType(strategy))
            .thenCompose {
                offerSearchService.addEventTo(event.data!!, searchResultId, getStrategyType(strategy))
            }.exceptionally { e ->
                logger.error("Request: addEvent/$event raised $e")
                throw e
            }
    }

    /**
     * Complain to Offer or search result item.
     */
    @ApiOperation("[Legacy] Updates status of the selected SearchResults item to <reject>.")
    @ApiResponses(value = [ApiResponse(code = 200, message = "Success")])
    @RequestMapping(method = [RequestMethod.PATCH], value = ["/result/{id}"])
    fun complain(
        @ApiParam("id of search result item")
        @PathVariable(value = "id")
        searchResultId: Long,

        @ApiParam("where client sends searchResult id and signature of the message.", required = true)
        @RequestBody
        request: SignedRequest<Long>,

        @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
        @RequestHeader("Strategy", required = false)
        strategy: String?
    ): CompletableFuture<Void> {

        return accountService.accountBySigMessage(request, getStrategyType(strategy))
            .thenAcceptAsync {
                offerSearchService.complain(request.data!!, it.publicKey, getStrategyType(strategy)).get()
                accountService.incrementNonce(it, getStrategyType(strategy)).get()
            }.exceptionally { e ->
                logger.error("Request: complain/$request raised $e")
                throw e
            }
    }

    /**
     * Reject Search Result
     */
    @ApiOperation("Updates status of the selected SearchResults item to <reject>.")
    @ApiResponses(value = [ApiResponse(code = 200, message = "Success")])
    @RequestMapping(method = [RequestMethod.PATCH], value = ["/result/reject/{id}"])
    fun reject(
        @ApiParam("id of search result item")
        @PathVariable(value = "id")
        searchResultId: Long,

        @ApiParam("where client sends searchResult id and signature of the message.", required = true)
        @RequestBody
        request: SignedRequest<Long>,

        @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
        @RequestHeader("Strategy", required = false)
        strategy: String?
    ): CompletableFuture<Void> {

        return accountService.accountBySigMessage(request, getStrategyType(strategy))
            .thenAcceptAsync {
                offerSearchService.reject(request.data!!, it.publicKey, getStrategyType(strategy)).get()
                accountService.incrementNonce(it, getStrategyType(strategy)).get()
            }.exceptionally { e ->
                logger.error("Request: reject/$request raised $e")
                throw e
            }
    }

    /**
     * Marks Search Result item for <evaluation>
     */
    @ApiOperation("Updates status of the selected SearchResults item to <evaluate>.")
    @ApiResponses(value = [ApiResponse(code = 200, message = "Success")])
    @RequestMapping(method = [RequestMethod.PATCH], value = ["/result/evaluate/{id}"])
    fun evaluate(
        @ApiParam("id of search result item")
        @PathVariable(value = "id")
        searchResultId: Long,

        @ApiParam("where client sends searchResult id and signature of the message.", required = true)
        @RequestBody
        request: SignedRequest<Long>,

        @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
        @RequestHeader("Strategy", required = false)
        strategy: String?
    ): CompletableFuture<Void> {

        return accountService.accountBySigMessage(request, getStrategyType(strategy))
            .thenAcceptAsync {
                offerSearchService.evaluate(request.data!!, it.publicKey, getStrategyType(strategy)).get()
                accountService.incrementNonce(it, getStrategyType(strategy)).get()
            }.exceptionally { e ->
                logger.error("Request: evaluate/$request raised $e")
                throw e
            }
    }

    /**
     * Marks Search Result as item that was purchased by user but not confirmed by vendor yet
     */
    @ApiOperation("Updates status of the selected SearchResults item to <claim purchase>.")
    @ApiResponses(value = [ApiResponse(code = 200, message = "Success")])
    @RequestMapping(method = [RequestMethod.PATCH], value = ["/result/claimpurchase/{id}"])
    fun claimPurchase(
        @ApiParam("id of search result item")
        @PathVariable(value = "id")
        searchResultId: Long,

        @ApiParam("where client sends searchResult id and signature of the message.", required = true)
        @RequestBody
        request: SignedRequest<Long>,

        @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
        @RequestHeader("Strategy", required = false)
        strategy: String?
    ): CompletableFuture<Void> {

        return accountService.accountBySigMessage(request, getStrategyType(strategy))
            .thenAcceptAsync {
                offerSearchService.claimPurchase(request.data!!, it.publicKey, getStrategyType(strategy)).get()
                accountService.incrementNonce(it, getStrategyType(strategy)).get()
            }.exceptionally { e ->
                logger.error("Request: claimPurchase/$request raised $e")
                throw e
            }
    }

    @ApiOperation("Updates status of the selected SearchResults item to <confirmed>.")
    @ApiResponses(value = [ApiResponse(code = 200, message = "Success")])
    @RequestMapping(method = [RequestMethod.PATCH], value = ["/result/confirm/{id}"])
    fun confirm(
        @ApiParam("id of search result item")
        @PathVariable(value = "id")
        searchResultId: Long,

        @ApiParam("where client sends searchResult id and signature of the message.", required = true)
        @RequestBody
        request: SignedRequest<Long>,

        @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
        @RequestHeader("Strategy", required = false)
        strategy: String?
    ): CompletableFuture<Void> {

        return accountService.accountBySigMessage(request, getStrategyType(strategy))
            .thenAcceptAsync {
                offerSearchService.confirm(request.data!!, it.publicKey, getStrategyType(strategy)).get()
                accountService.incrementNonce(it, getStrategyType(strategy)).get()
            }.exceptionally { e ->
                logger.error("Request: confirm/$request raised $e")
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
    @RequestMapping(method = [RequestMethod.PUT], value = ["/result/{owner}/{id}"])
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
        strategy: String?
    ): CompletableFuture<List<OfferSearch>> {

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
                logger.error("Request: cloneOfferSearchOfSearchRequest/$request raised $e")
                throw e
            }
    }
}
