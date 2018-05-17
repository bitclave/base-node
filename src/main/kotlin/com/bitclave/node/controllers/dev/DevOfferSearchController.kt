package com.bitclave.node.controllers.dev

import com.bitclave.node.controllers.AbstractController
import com.bitclave.node.repository.models.OfferSearch
import com.bitclave.node.repository.models.SignedRequest
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
@RequestMapping("/dev/client/{clientId}/search/result/")
class DevOfferSearchController(
        @Qualifier("v1") private val accountService: AccountService,
        @Qualifier("v1") private val offerSearchService: OfferSearchService
) : AbstractController() {

    /**
     * Add offer to search result for some search request
     *
     */
    @ApiOperation("Add offer to search result for some search request")
    @ApiResponses(value = [ApiResponse(code = 201, message = "Created")])
    @RequestMapping(method = [RequestMethod.POST])
    @ResponseStatus(HttpStatus.CREATED)
    fun putOfferSearchItem(
            @ApiParam("public key of client")
            @PathVariable(value = "clientId")
            clientId: String,

            @ApiParam("where client sends offer search model and signature of the message.")
            @RequestBody
            request: SignedRequest<OfferSearch>,

            @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
            @RequestHeader("Strategy", required = false)
            strategy: String?): CompletableFuture<Void> {

        return accountService.accountBySigMessage(request, getStrategyType(strategy))
                .thenCompose {
                    if (clientId != it.publicKey) {
                        throw AccessDeniedException()
                    }

                    offerSearchService.saveOfferSearch(it.publicKey, request.data!!, getStrategyType(strategy))
                }
    }

}
