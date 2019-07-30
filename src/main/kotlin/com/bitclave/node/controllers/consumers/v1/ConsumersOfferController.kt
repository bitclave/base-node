package com.bitclave.node.controllers.consumers.v1

import com.bitclave.node.controllers.AbstractController
import com.bitclave.node.repository.models.Offer
import com.bitclave.node.services.v1.OfferService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.CompletableFuture

enum class OfferFields {
    COMPARE,
    RULE,
    PRICE
}

@RestController
@RequestMapping("/v1/consumers/")
class ConsumersOfferController(
    @Qualifier("v1") private val offerService: OfferService
) : AbstractController() {

    private val logger = KotlinLogging.logger {}

    @ApiOperation(
        "Page through already created offers by excluding products",
        response = Offer::class,
        responseContainer = "Slice"
    )
    @ApiResponses(
        value = [
            ApiResponse(code = 200, message = "Success", response = Slice::class)
        ]
    )
    @RequestMapping(value = ["offers"], method = [RequestMethod.GET], params = ["page", "size"])
    fun getConsumersOffers(
        @ApiParam("Optional page number to retrieve a particular page. If not specified this API retrieves first page.")
        @RequestParam("page", defaultValue = "0")
        page: Int,

        @ApiParam("Optional page size to include number of offers in a page. Defaults to 20.")
        @RequestParam("size", defaultValue = "100")
        size: Int,

        @ApiParam("Optional load data of compare field")
        @RequestParam("fields", defaultValue = "", required = false)
        fields: Set<OfferFields>,

        @ApiParam("Optional except by type of offers")
        @RequestParam("except", required = false)
        exceptType: Offer.OfferType?,

        @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
        @RequestHeader("Strategy", required = false)
        strategy: String?
    ): CompletableFuture<Slice<Offer>> {

        return offerService.getConsumersOffers(
            PageRequest.of(page, size),
            fields.contains(OfferFields.COMPARE),
            fields.contains(OfferFields.RULE),
            fields.contains(OfferFields.PRICE),
            getStrategyType(strategy),
            exceptType
        ).exceptionally { e ->
            logger.error("Request: getConsumersOffers/$page/$size raised $e")
            throw e
        }
    }
}
