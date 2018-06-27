package com.bitclave.node.controllers.v1

import com.bitclave.node.controllers.AbstractController
import com.bitclave.node.repository.models.Account
import com.bitclave.node.repository.models.SignedRequest
import com.bitclave.node.repository.models.Site
import com.bitclave.node.services.errors.BadArgumentException
import com.bitclave.node.services.v1.AccountService
import com.bitclave.node.services.v1.SiteService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.bind.annotation.*
import java.util.concurrent.CompletableFuture

@RestController
@RequestMapping("/v1/site/")
class SiteController(
        @Qualifier("v1") private val accountService: AccountService,
        @Qualifier("v1") private val siteService: SiteService
) : AbstractController() {

    /**
     * Save information of site.
     * @param request is {@link SignedRequest} where client sends {@link Site} and
     * signature of the message.
     *
     * @return {@link Long}, Http status - 200.
     *
     * @exception   {@link BadArgumentException} - 400
     *              {@link AccessDeniedException} - 403
     *              {@link DataNotSaved} - 500
     */

    @ApiOperation("Save information of site",
            response = Long::class)
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Success", response = Long::class),
        ApiResponse(code = 400, message = "BadArgumentException"),
        ApiResponse(code = 403, message = "AccessDeniedException"),
        ApiResponse(code = 500, message = "DataNotSaved")
    ])
    //@RequestMapping(method = [RequestMethod.POST])
    fun saveSiteInformation(
            @ApiParam("where client sends SearchRequest and signature of the message.", required = true)
            @RequestBody
            request: SignedRequest<Site>,

            @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
            @RequestHeader("Strategy", required = false)
            strategy: String?
    ): CompletableFuture<Long> {

        return accountService.accountBySigMessage(request, getStrategyType(strategy))
                .thenCompose { account: Account -> accountService.validateNonce(request, account) }
                .thenCompose {
                    if (it.publicKey != request.data!!.publicKey) {
                        throw BadArgumentException()
                    }

                    val result = siteService.saveSiteInformation(
                            request.data,
                            getStrategyType(strategy)
                    )

                    accountService.incrementNonce(it, getStrategyType(strategy))

                    result
                }
    }

    /**
     * Get information of site by origin.
     *
     * @return {@link Site}, Http status - 200.
     *
     * @exception   {@link NotFoundException} - 404
     */
    @ApiOperation(
            "Get {@link Site} by origin of site",
            response = Site::class
    )
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Success", response = Site::class),
        ApiResponse(code = 404, message = "NotFoundException")
    ])
    @RequestMapping(method = [RequestMethod.GET], value = ["{origin:.+}"])
    fun getPublicKeyByOrigin(
            @ApiParam("Site origin")
            @PathVariable("origin")
            origin: String,

            @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
            @RequestHeader("Strategy", required = false)
            strategy: String?
    ): CompletableFuture<Site> {

        return siteService.getSite(origin, getStrategyType(strategy))
    }

}
