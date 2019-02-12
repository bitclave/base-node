package com.bitclave.node.controllers.dev

import com.bitclave.node.controllers.AbstractController
import com.bitclave.node.repository.models.*
import com.bitclave.node.services.errors.AccessDeniedException
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
    @RequestMapping(method = [RequestMethod.GET], value = ["/offersearch/ids"])
    fun getOfferSearchesByIds(
            @ApiParam("ids of search requests")
            @RequestBody
            ids: List<Long>,

            @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
            @RequestHeader("Strategy", required = false)
            strategy: String?): CompletableFuture<List<OfferSearch>> {

        return offerSearchService.getOfferSearchesByIds(
                getStrategyType(strategy),
                ids
        )
    }

}
