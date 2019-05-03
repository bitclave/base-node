package com.bitclave.node.controllers.dev

import com.bitclave.node.controllers.AbstractController
import com.bitclave.node.repository.models.Account
import com.bitclave.node.repository.models.OfferSearch
import com.bitclave.node.repository.models.SignedRequest
import com.bitclave.node.services.v1.AccountService
import com.bitclave.node.services.v1.OfferSearchService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import java.util.Date
import java.util.concurrent.CompletableFuture

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/dev/verify")
class VerifyConsistencyController(
    @Qualifier("v1") private val accountService: AccountService,
    @Qualifier("v1") private val offerSearchService: OfferSearchService
) : AbstractController() {

    /**
     * Returns the OfferSearches by provided ids.
     *
     * @return {@link List<OfferSearch>}, Http status - 200.
     *
     */
    @ApiOperation(
        "Returns the OfferSearches by provided ids.",
        response = OfferSearch::class, responseContainer = "List"
    )
    @ApiResponses(
        value = [
            ApiResponse(code = 200, message = "Success", response = List::class)
        ]
    )
    @RequestMapping(method = [RequestMethod.POST], value = ["/offersearch/ids"])
    fun getOfferSearchesByIds(
        @ApiParam("ids of search requests", required = true)
        @RequestBody
        request: SignedRequest<List<Long>>,

        @ApiParam(
            "change repository strategy",
            allowableValues = "POSTGRES, HYBRID",
            required = false
        )
        @RequestHeader("Strategy", required = false)
        strategy: String?
    ): CompletableFuture<List<OfferSearch>> {
        return accountService
            .accountBySigMessage(request, getStrategyType(strategy))
            .thenCompose {
                offerSearchService.getOfferSearchesByIds(
                    getStrategyType(strategy),
                    request.data!!
                )
            }.exceptionally { e ->
                logger.error("Request: $request raised $e")
                throw e
            }
    }

    /**
     * Returns the Accounts by provided publicKeys.
     *
     * @return {@link List<Account>}, Http status - 200.
     *
     */
    @ApiOperation(
        "Returns the Accounts by provided publicKeys.",
        response = Account::class, responseContainer = "List"
    )
    @ApiResponses(
        value = [
            ApiResponse(code = 200, message = "Success", response = List::class)
        ]
    )
    @RequestMapping(method = [RequestMethod.POST], value = ["/account/publickeys"])
    fun getAccountsByPublicKeys(
        @ApiParam("ids of search requests", required = true)
        @RequestBody
        request: SignedRequest<List<String>>,

        @ApiParam(
            "change repository strategy",
            allowableValues = "POSTGRES, HYBRID",
            required = false
        )
        @RequestHeader("Strategy", required = false)
        strategy: String?
    ): CompletableFuture<List<Account>> {
        return accountService
            .accountBySigMessage(request, getStrategyType(strategy))
            .thenCompose {
                accountService.getAccounts(
                    getStrategyType(strategy),
                    request.data!!
                )
            }.exceptionally { e ->
                logger.error("Request: $request raised $e")
                throw e
            }
    }

    /**
     * Returns all the Accounts.
     *
     * @return {@link List<Account>}, Http status - 200.
     *
     */
    @ApiOperation(
        "Returns all the Accounts.",
        response = Account::class, responseContainer = "List"
    )
    @ApiResponses(
        value = [
            ApiResponse(code = 200, message = "Success", response = List::class)
        ]
    )
    @RequestMapping(method = [RequestMethod.POST], value = ["/account/all"])
    fun getAllAccounts(
        @ApiParam("fromDate for filtering", required = true)
        @RequestBody
        request: SignedRequest<Date>,

        @ApiParam(
            "change repository strategy",
            allowableValues = "POSTGRES, HYBRID",
            required = false
        )
        @RequestHeader("Strategy", required = false)
        strategy: String?
    ): CompletableFuture<List<Account>> {
        return accountService
            .accountBySigMessage(request, getStrategyType(strategy))
            .thenCompose {
                accountService.getAllAccounts(
                    getStrategyType(strategy),
                    request.data!!
                )
            }.exceptionally { e ->
                logger.error("Request: getAllAccounts raised $e")
                throw e
            }
    }

    /**
     * Returns the dangling OfferSearches by Offers
     *
     * @return {@link List<OfferSearch>}, Http status - 200.
     *
     */
    @ApiOperation(
        "Returns the dangling OfferSearches by Offers",
        response = OfferSearch::class, responseContainer = "List"
    )
    @ApiResponses(
        value = [
            ApiResponse(code = 200, message = "Success", response = List::class)
        ]
    )
    @RequestMapping(method = [RequestMethod.GET], value = ["/offersearch/byOffer"])
    fun getDanglingOfferSearchesByOffer(

        @ApiParam(
            "change repository strategy",
            allowableValues = "POSTGRES, HYBRID",
            required = false
        )
        @RequestHeader("Strategy", required = false)
        strategy: String?
    ): CompletableFuture<List<OfferSearch>> {

        return offerSearchService.getDanglingOfferSearches(
            getStrategyType(strategy),
            byOffer = true,
            bySearchRequest = false
        ).exceptionally { e ->
            logger.error("Request: getDanglingOfferSearchesByOffer raised $e")
            throw e
        }
    }

    /**
     * Returns the dangling OfferSearches by SearchRequests
     *
     * @return {@link List<OfferSearch>}, Http status - 200.
     *
     */
    @ApiOperation(
        "Returns the dangling OfferSearches by SearchRequests",
        response = OfferSearch::class, responseContainer = "List"
    )
    @ApiResponses(
        value = [
            ApiResponse(code = 200, message = "Success", response = List::class)
        ]
    )
    @RequestMapping(method = [RequestMethod.GET], value = ["/offersearch/bySearchRequest"])
    fun getDanglingOfferSearchesBySearchRequest(

        @ApiParam(
            "change repository strategy",
            allowableValues = "POSTGRES, HYBRID",
            required = false
        )
        @RequestHeader("Strategy", required = false)
        strategy: String?
    ): CompletableFuture<List<OfferSearch>> {

        return offerSearchService.getDanglingOfferSearches(
            getStrategyType(strategy),
            byOffer = false,
            bySearchRequest = true
        ).exceptionally { e ->
            logger.error("Request: getDanglingOfferSearchesBySearchRequest raised $e")
            throw e
        }
    }

    /**
     * Returns offerSearches with the same owner and offerId but different content (status/events)
     *
     * @return {@link List<OfferSearch>}, Http status - 200.
     *
     */
    @ApiOperation(
        "Returns offerSearches with the same owner and offerId but different content (status/events)",
        response = OfferSearch::class, responseContainer = "List"
    )
    @ApiResponses(
        value = [
            ApiResponse(code = 200, message = "Success", response = List::class)
        ]
    )
    @RequestMapping(method = [RequestMethod.GET], value = ["/offersearch/conflicted"])
    fun getDiffOfferSearches(

        @ApiParam(
            "change repository strategy",
            allowableValues = "POSTGRES, HYBRID",
            required = false
        )
        @RequestHeader("Strategy", required = false)
        strategy: String?
    ): CompletableFuture<List<OfferSearch>> {

        return offerSearchService.getDiffOfferSearches(getStrategyType(strategy))
            .exceptionally { e ->
                logger.error("Request: getDiffOfferSearches raised $e")
                throw e
            }
    }
}
