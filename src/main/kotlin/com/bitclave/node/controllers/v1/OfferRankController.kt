package com.bitclave.node.controllers.v1

import com.bitclave.node.controllers.AbstractController
import com.bitclave.node.services.v1.AccountService
import com.bitclave.node.repository.models.Account
import com.bitclave.node.repository.models.OfferRank
import com.bitclave.node.repository.models.SignedRequest
import com.bitclave.node.services.v1.OfferRankService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

import java.util.concurrent.CompletableFuture

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/v1/offerRank")
class OfferRankController(
    @Qualifier("v1") private val offerRankService: OfferRankService,
    @Qualifier("v1") private val accountService: AccountService
) : AbstractController() {

    @ApiOperation(
        "Create new OfferRank",
        response = Long::class
    )
    @ApiResponses(
        value = [
            ApiResponse(code = 200, message = "Success", response = Long::class),
            ApiResponse(code = 400, message = "BadArgumentException"),
            ApiResponse(code = 403, message = "AccessDeniedException"),
            ApiResponse(code = 500, message = "DataNotSaved")
        ]
    )
    @RequestMapping(method = [RequestMethod.POST])
    fun createOfferRank(
        @ApiParam("where client sends rank, rankId, offerId in the message.", required = true)
        @RequestBody
        request: SignedRequest<OfferRank>,

        @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
        @RequestHeader("Strategy", required = false)
        strategy: String?
    ): CompletableFuture<OfferRank> {
        return accountService
            .accountBySigMessage(request, getStrategyType(strategy))
            .thenCompose { account: Account -> accountService.validateNonce(request, account) }
            .thenCompose {
                val offerRank = request.data
                offerRankService.createOfferRank(getStrategyType(strategy), offerRank!!)
            }.exceptionally { e ->
                logger.error("Request: saveSiteInformation/$request raised $e")
                throw e
            }
    }

    @ApiOperation(
        "Update OfferRank",
        response = Long::class
    )
    @ApiResponses(
        value = [
            ApiResponse(code = 200, message = "Success", response = Long::class),
            ApiResponse(code = 400, message = "BadArgumentException"),
            ApiResponse(code = 403, message = "AccessDeniedException"),
            ApiResponse(code = 500, message = "DataNotSaved")
        ]
    )
    @RequestMapping(method = [RequestMethod.PUT])
    fun updateOfferRank(
        @ApiParam("where client sends rank, rankId, offerId in the message.", required = true)
        @RequestBody
        request: SignedRequest<OfferRank>,

        @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
        @RequestHeader("Strategy", required = false)
        strategy: String?
    ): CompletableFuture<OfferRank> {
        return accountService
            .accountBySigMessage(request, getStrategyType(strategy))
            .thenCompose { account: Account -> accountService.validateNonce(request, account) }
            .thenCompose {
                val offerRank = request.data
                offerRankService.updateOfferRank(getStrategyType(strategy), offerRank!!)
            }
    }

    @ApiOperation(
        "get offerRanks by offerId", response = OfferRank::class, responseContainer = "List"
    )
    @ApiResponses(
        value = [
            ApiResponse(code = 200, message = "Success", response = List::class)
        ]
    )
    @RequestMapping(method = [RequestMethod.GET], value = ["/", "{id}"])
    fun getOfferRanksByOfferId(

        @ApiParam("Optional offerId.")
        @PathVariable(value = "id", required = false)
        offerId: Long?,

        @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
        @RequestHeader("Strategy", required = false)
        strategy: String?
    ): CompletableFuture<List<OfferRank>> {

        return offerRankService
            .getOfferRanksByOfferId(getStrategyType(strategy), offerId)
            .exceptionally { e ->
                logger.error("Request: getOfferRanks by ooferId /$offerId raised $e")
                throw e
            }
    }
}
