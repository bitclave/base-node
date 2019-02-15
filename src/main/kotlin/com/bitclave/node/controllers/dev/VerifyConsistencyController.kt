package com.bitclave.node.controllers.dev

import com.bitclave.node.controllers.AbstractController
import com.bitclave.node.repository.models.*
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
    @ApiOperation("Returns the OfferSearches by provided ids.",
            response = OfferSearch::class, responseContainer = "List")
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Success", response = List::class)
    ])
    @RequestMapping(method = [RequestMethod.POST], value = ["/offersearch/ids"])
    fun getOfferSearchesByIds(
            @ApiParam("ids of search requests", required = true)
            @RequestBody
            request: SignedRequest<List<Long>>,

            @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
            @RequestHeader("Strategy", required = false)
            strategy: String?): CompletableFuture<List<OfferSearch>> {

        return accountService
                .accountBySigMessage(request, getStrategyType(strategy))
                .thenCompose {
                    offerSearchService.getOfferSearchesByIds(
                            getStrategyType(strategy),
                            request.data!!
                    )
                }
    }

    /**
     * Returns the Accounts by provided publicKeys.
     *
     * @return {@link List<Account>}, Http status - 200.
     *
     */
    @ApiOperation("Returns the Accounts by provided publicKeys.",
            response = Account::class, responseContainer = "List")
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Success", response = List::class)
    ])
    @RequestMapping(method = [RequestMethod.POST], value = ["/account/publickeys"])
    fun getAccountsByPublicKeys(
            @ApiParam("ids of search requests", required = true)
            @RequestBody
            request: SignedRequest<List<String>>,

            @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
            @RequestHeader("Strategy", required = false)
            strategy: String?): CompletableFuture<List<Account>> {

        return accountService
                .accountBySigMessage(request, getStrategyType(strategy))
                .thenCompose {
                    accountService.getAccounts(
                            getStrategyType(strategy),
                            request.data!!
                    )
                }
    }

}
