package com.bitclave.node.controllers.consumers.v1

import com.bitclave.node.controllers.AbstractController
import com.bitclave.node.repository.entities.OfferSearch
import com.bitclave.node.services.v1.OfferSearchService
import com.bitclave.node.utils.Logger
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.CompletableFuture

@RestController
@RequestMapping("/v1/consumers/")
class ConsumersOfferSearchController(
    @Qualifier("v1") private val offerSearchService: OfferSearchService
) : AbstractController() {

    @ApiOperation(
        "Slice through already created offersearch results",
        response = OfferSearch::class, responseContainer = "Slice"
    )
    @ApiResponses(
        value = [
            ApiResponse(code = 200, message = "Success", response = Slice::class)
        ]
    )
    @RequestMapping(value = ["search/results"], method = [RequestMethod.GET], params = ["page", "size"])
    fun getConsumersOfferSearch(
        @ApiParam("Optional page number to retrieve a particular page. If not specified this API retrieves first page.")
        @RequestParam("page", defaultValue = "0")
        page: Int,

        @ApiParam("Optional page size to include number of OfferSearch in a page. Defaults to 100.")
        @RequestParam("size", defaultValue = "100")
        size: Int,

        @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
        @RequestHeader("Strategy", required = false)
        strategy: String?
    ): CompletableFuture<Slice<OfferSearch>> {
        return offerSearchService.getConsumersOfferSearches(
            PageRequest.of(page, size, Sort.by(Sort.Order(Sort.Direction.ASC, "id"))), getStrategyType(strategy)
        ).exceptionally { e ->
            Logger.error("Request: getConsumersOfferSearch/$page/$size raised", e)
            throw e
        }
    }

    @ApiOperation(
        "Slice through already created offersearch results by owners",
        response = OfferSearch::class, responseContainer = "Slice"
    )
    @ApiResponses(
        value = [
            ApiResponse(code = 200, message = "Success", response = Slice::class)
        ]
    )
    @RequestMapping(value = ["search/results"], method = [RequestMethod.POST], params = ["page", "size"])
    fun getConsumersOfferSearchBySearchRequestIds(
        @ApiParam("Optional page number to retrieve a particular page. If not specified this API retrieves first page.")
        @RequestParam("page", defaultValue = "0")
        page: Int,

        @ApiParam("Optional page size to include number of OfferSearch in a page. Defaults to 100.")
        @RequestParam("size", defaultValue = "100")
        size: Int,

        @ApiParam("where client sends ids of searchRequests", required = true)
        @RequestBody
        ids: List<Long>,

        @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
        @RequestHeader("Strategy", required = false)
        strategy: String?
    ): CompletableFuture<Slice<OfferSearch>> {
        return offerSearchService
            .getConsumersOfferSearchesBySearchRequestIds(
                ids,
                PageRequest.of(page, size, Sort.by(Sort.Order(Sort.Direction.ASC, "id"))),
                getStrategyType(strategy)
            )
            .exceptionally { e ->
                Logger.error("Request: getConsumersOfferSearchBySearchRequestIds/$page/$size/$ids raised", e)
                throw e
            }
    }
}
