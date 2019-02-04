package com.bitclave.node.controllers.v1

import com.bitclave.node.controllers.AbstractController
import com.bitclave.node.repository.models.Offer
import com.bitclave.node.repository.models.OfferSearch
import com.bitclave.node.repository.models.SearchRequest
import com.bitclave.node.services.v1.OfferSearchService
import com.bitclave.node.services.v1.OfferService
import com.bitclave.node.services.v1.SearchRequestService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.*
import java.util.concurrent.CompletableFuture

@RestController
@RequestMapping("/v1/")
class PageableController(
        @Qualifier("v1") private val offerService: OfferService,
        @Qualifier("v1") private val searchRequestService: SearchRequestService,
        @Qualifier("v1") private val offerSearchService: OfferSearchService
) : AbstractController() {

    @ApiOperation(
            "Page through already created offers", response = Offer::class, responseContainer = "Page"
    )
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Success", response = Page::class)
    ])
    @RequestMapping(value = "offers", method = [RequestMethod.GET], params = [ "page", "size"])
    fun getPageableOffers(
        @ApiParam("Optional page number to retrieve a particular page. If not specified this API retrieves first page.")
        @RequestParam( "page" )
        page: Int?,

        @ApiParam("Optional page size to include number of offers in a page. Defaults to 20.")
        @RequestParam( "size" )
        size: Int?,

        @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
        @RequestHeader("Strategy", required = false)
        strategy: String?): CompletableFuture<Page<Offer>> {

        if(page == null || size == null) {
            return offerService.getPageableOffers(PageRequest(0, 20), getStrategyType(strategy)
            )
        }

        return offerService.getPageableOffers(PageRequest(page, size), getStrategyType(strategy))
    }

    @ApiOperation(
            "Page through already created search requests",
            response = SearchRequest::class, responseContainer = "Page"
    )
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Success", response = Page::class)
    ])
    @RequestMapping(value = "search/requests", method = [RequestMethod.GET], params = [ "page", "size"])
    fun getPageableSearchRequests(
            @ApiParam("Optional page number to retrieve a particular page. If not specified this API retrieves first page.")
            @RequestParam( "page" )
            page: Int?,

            @ApiParam("Optional page size to include number of offers in a page. Defaults to 20.")
            @RequestParam( "size" )
            size: Int?,

            @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
            @RequestHeader("Strategy", required = false)
            strategy: String?): CompletableFuture<Page<SearchRequest>> {

        if(page == null || size == null) {
            return searchRequestService.getPageableRequests(
                    PageRequest(0, 20), getStrategyType(strategy)
            )
        }

        return searchRequestService.getPageableRequests(
                PageRequest(page, size), getStrategyType(strategy))
    }

    @ApiOperation(
            "Page through already created offersearch results",
            response = OfferSearch::class, responseContainer = "Page"
    )
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Success", response = Page::class)
    ])
    @RequestMapping(value = "search/results", method = [RequestMethod.GET], params = [ "page", "size"])
    fun getPageableOfferSearch(
            @ApiParam("Optional page number to retrieve a particular page. If not specified this API retrieves first page.")
            @RequestParam( "page" )
            page: Int?,

            @ApiParam("Optional page size to include number of offers in a page. Defaults to 20.")
            @RequestParam( "size" )
            size: Int?,

            @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
            @RequestHeader("Strategy", required = false)
            strategy: String?): CompletableFuture<Page<OfferSearch>> {

        if(page == null || size == null) {
            return offerSearchService.getPageableOfferSearches(
                    PageRequest(0, 20), getStrategyType(strategy)
            )
        }

        return offerSearchService.getPageableOfferSearches(
                PageRequest(page, size), getStrategyType(strategy))
    }
}