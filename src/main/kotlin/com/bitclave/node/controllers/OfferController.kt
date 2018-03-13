package com.bitclave.node.controllers

import com.bitclave.node.repository.models.Offer
import com.bitclave.node.repository.models.SignedRequest
import com.bitclave.node.services.AccountService
import com.bitclave.node.services.OfferService
import com.bitclave.node.services.errors.BadArgumentException
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.concurrent.CompletableFuture

@RestController
@RequestMapping("client/{owner}/offer")
class OfferController(
        private val accountService: AccountService,
        private val offerService: OfferService
) : AbstractController() {

    /**
     * Creates new or update a offer in the system, based on the provided information.
     * The API will verify that the request is cryptographically signed by the owner of the public key.
     * @param request is {@link SignedRequest} where client sends {@link Offer} and
     * signature of the message.
     *
     * @return {@link Offer}, Http status - 200/201.
     *
     * @exception   {@link BadArgumentException} - 400
     *              {@link AccessDeniedException} - 403
     *              {@link DataNotSaved} - 500
     */

    @ApiOperation("Creates a new offer in the system, based on the provided information.\n" +
            "The API will verify that the request is cryptographically signed by the owner of the public key.",
            response = Offer::class)
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Updated", response = Offer::class),
        ApiResponse(code = 201, message = "Created", response = Offer::class),
        ApiResponse(code = 400, message = "BadArgumentException"),
        ApiResponse(code = 403, message = "AccessDeniedException"),
        ApiResponse(code = 500, message = "DataNotSaved")
    ])
    @RequestMapping(method = [RequestMethod.PUT], value = ["/", "{id}"])
    fun putOffer(
            @ApiParam("public key owner of offer")
            @PathVariable(value = "owner")
            owner: String,

            @ApiParam("Optional id of already created a offer. Use for update offer")
            @PathVariable(value = "id", required = false)
            id: Long?,

            @ApiParam("where client sends Offer and signature of the message.", required = true)
            @RequestBody
            request: SignedRequest<Offer>,

            @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
            @RequestHeader("Strategy", required = false)
            strategy: String?): CompletableFuture<ResponseEntity<Offer>> {

        return accountService.accountBySigMessage(request, getStrategyType(strategy))
                .thenCompose {
                    if (request.pk != owner) {
                        throw BadArgumentException()
                    }
                    offerService.putOffer(
                            id ?: 0,
                            owner,
                            request.data!!,
                            getStrategyType(strategy)
                    )
                }.thenCompose {
                    val status = if (it.id != id) HttpStatus.CREATED else HttpStatus.OK
                    CompletableFuture.completedFuture(ResponseEntity<Offer>(it, status))
                }
    }

    /**
     * Delete a offer from the system.
     * The API will verify that the request is cryptographically signed by the owner of the public key.
     * @param request is {@link SignedRequest} where client sends {@link Long} and
     * signature of the message.
     *
     * @return {@link Offer}, Http status - 200.
     *
     * @exception   {@link BadArgumentException} - 400
     *              {@link AccessDeniedException} - 403
     *              {@link DataNotSaved} - 500
     */

    @ApiOperation("Delete a offer from the system.\n" +
            "The API will verify that the request is cryptographically signed by the owner of the public key.",
            response = Long::class)
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Deleted", response = Long::class),
        ApiResponse(code = 400, message = "BadArgumentException"),
        ApiResponse(code = 403, message = "AccessDeniedException"),
        ApiResponse(code = 404, message = "NotFoundException")
    ])
    @RequestMapping(method = [RequestMethod.DELETE], value = ["{id}"])
    fun deleteOffer(
            @ApiParam("public key owner of offer")
            @PathVariable(value = "owner")
            owner: String,

            @ApiParam("id of existed offer.")
            @PathVariable(value = "id")
            id: Long,

            @ApiParam("where client sends Offer id and signature of the message.", required = true)
            @RequestBody
            request: SignedRequest<Long>,

            @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
            @RequestHeader("Strategy", required = false)
            strategy: String?): CompletableFuture<Long> {

        return accountService.accountBySigMessage(request, getStrategyType(strategy))
                .thenCompose {
                    if (request.pk != owner || id != request.data) {
                        throw BadArgumentException()
                    }
                    offerService.deleteOffer(
                            id,
                            owner,
                            getStrategyType(strategy)
                    )
                }
    }

    /**
     * Return list of already created offers by owner (Public key of creator).
     *
     * @return {@link List<Offer>}, Http status - 200.
     */
    @ApiOperation(
            "get already created offers", response = Offer::class, responseContainer = "List"
    )
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Success", response = List::class)
    ])
    @RequestMapping(method = [RequestMethod.GET], value = ["/", "{id}"])
    fun getOffer(
            @ApiParam("owner who create offer(s)", required = true)
            @PathVariable("owner", required = true)
            owner: String,

            @ApiParam("Optional id of already created a offer.")
            @PathVariable(value = "id", required = false)
            id: Long?,

            @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
            @RequestHeader("Strategy", required = false)
            strategy: String?): CompletableFuture<List<Offer>> {

        return offerService.getOffers(id ?: 0, owner, getStrategyType(strategy))
    }

}
