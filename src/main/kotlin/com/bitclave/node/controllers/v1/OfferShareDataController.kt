package com.bitclave.node.controllers.v1

import com.bitclave.node.controllers.AbstractController
import com.bitclave.node.repository.models.Account
import com.bitclave.node.repository.models.OfferShareData
import com.bitclave.node.repository.models.SignedRequest
import com.bitclave.node.services.v1.AccountService
import com.bitclave.node.services.v1.OfferShareService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.util.concurrent.CompletableFuture

@RestController
@RequestMapping("/v1/data/")
class OfferShareDataController(
        @Qualifier("v1") private val accountService: AccountService,
        @Qualifier("v1") private val offerShareData: OfferShareService
) : AbstractController() {

    /**
     * Returns a list of shared data for offer.
     * @param accepted - accepted by Business or not.
     *
     * @return List of {@link OfferShareData}, or empty list. Http status - 200.
     */
    @ApiOperation("Returns a list of requests",
            response = OfferShareData::class, responseContainer = "List")
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Success", response = OfferShareData::class,
                responseContainer = "List")
    ])
    @RequestMapping(method = [RequestMethod.GET], value = ["offer/"])
    fun getShareData(
            @ApiParam("id of offer owner")
            @RequestParam("owner")
            offerOwner: String,

            @ApiParam("accepted or not", required = false)
            @RequestParam("accepted", required = false)
            accepted: Boolean?,

            @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
            @RequestHeader("Strategy", required = false)
            strategy: String?
    ): CompletableFuture<List<OfferShareData>> {

        return offerShareData.getShareData(offerOwner, accepted, getStrategyType(strategy))
    }

    /**
     * Grant access data for offer.
     * @param request info of request for privacy client data
     *
     * @return id of created request.
     *
     * @exception   {@link BadArgumentException} - 400
     *              {@link AccessDeniedException} - 403
     *              {@link NotFoundException} - 404
     *              {@link DuplicateException} - 409
     *              {@link DataNotSaved} - 500
     */
    @ApiOperation("Grant access data for offer")
    @ApiResponses(value = [
        ApiResponse(code = 201, message = "Created"),
        ApiResponse(code = 400, message = "BadArgumentException"),
        ApiResponse(code = 403, message = "AccessDeniedException"),
        ApiResponse(code = 404, message = "NotFoundException"),
        ApiResponse(code = 409, message = "DuplicateException"),
        ApiResponse(code = 500, message = "DataNotSaved")
    ])
    @RequestMapping(method = [RequestMethod.POST], value = ["grant/offer/"])
    @ResponseStatus(HttpStatus.CREATED)
    fun grantAccess(
            @ApiParam("Grant access data for offer", required = true)
            @RequestBody
            request: SignedRequest<OfferShareData>,

            @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
            @RequestHeader("Strategy", required = false)
            strategy: String?
    ): CompletableFuture<Void> {

        return accountService.accountBySigMessage(request, getStrategyType(strategy))
                .thenCompose { account: Account -> accountService.validateNonce(request, account) }
                .thenAcceptAsync({
                    offerShareData.grantAccess(it.publicKey, request.data!!, getStrategyType(strategy))
                            .get()

                    accountService.incrementNonce(it, getStrategyType(strategy)).get()
                })
    }

    /**
     * Business accepted what match offer data with client data and pay worth.
     * @param {@link SignedRequest} with encrypted data
     *
     * @return http status 202
     *
     * @exception   {@link BadArgumentException} - 400
     *              {@link AccessDeniedException} - 403
     *              {@link NotFoundException} - 404
     *              {@link DataNotSaved} - 500
     */
    @ApiOperation("Business accepted what match offer data with client data and pay worth.")
    @ApiResponses(value = [
        ApiResponse(code = 202, message = "Accepted"),
        ApiResponse(code = 400, message = "BadArgumentException"),
        ApiResponse(code = 403, message = "AccessDeniedException"),
        ApiResponse(code = 404, message = "NotFoundException"),
        ApiResponse(code = 500, message = "DataNotSaved")
    ])
    @RequestMapping(method = [RequestMethod.PATCH], value = ["offer/"])
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun accept(
            @ApiParam("id of offer", required = true)
            @RequestParam("offerSearchId")
            offerSearchId: Long,

            @ApiParam("SignedRequest with value of worth", required = true)
            @RequestBody request: SignedRequest<BigDecimal>,

            @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
            @RequestHeader("Strategy", required = false)
            strategy: String?
    ): CompletableFuture<Void> {

        return accountService.accountBySigMessage(request, getStrategyType(strategy))
                .thenCompose { account: Account -> accountService.validateNonce(request, account) }
                .thenAcceptAsync({
                    offerShareData.acceptShareData(
                            it.publicKey,
                            offerSearchId,
                            request.data!!,
                            getStrategyType(strategy)
                    ).get()

                    accountService.incrementNonce(it, getStrategyType(strategy)).get()
                })
    }

}
