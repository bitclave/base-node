package com.bitclave.node.controllers.v1

import com.bitclave.node.controllers.AbstractController
import com.bitclave.node.repository.models.Account
import com.bitclave.node.repository.models.SearchRequest
import com.bitclave.node.repository.models.SignedRequest
import com.bitclave.node.services.errors.AccessDeniedException
import com.bitclave.node.services.errors.BadArgumentException
import com.bitclave.node.services.v1.AccountService
import com.bitclave.node.services.v1.SearchRequestService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.concurrent.CompletableFuture

@RestController
@RequestMapping("/v1/client/{owner}/search/request/")
class SearchRequestController(
        @Qualifier("v1") private val accountService: AccountService,
        @Qualifier("v1") private val searchRequestService: SearchRequestService
) : AbstractController() {

    /**
     * Creates new request for search in the system, based on the provided information.
     * The API will verify that the request is cryptographically signed by the owner of the public key.
     * @param request is {@link SignedRequest} where client sends {@link SearchRequest} and
     * signature of the message.
     *
     * @return {@link Long}, Http status - 201.
     *
     * @exception   {@link BadArgumentException} - 400
     *              {@link AccessDeniedException} - 403
     *              {@link DataNotSaved} - 500
     */
    @ApiOperation("Creates new request for search in the system, based on the provided information.\n" +
            "The API will verify that the request is cryptographically signed by the owner of the public key.",
            response = SearchRequest::class)
    @ApiResponses(value = [
        ApiResponse(code = 201, message = "Created", response = SearchRequest::class),
        ApiResponse(code = 400, message = "BadArgumentException"),
        ApiResponse(code = 403, message = "AccessDeniedException"),
        ApiResponse(code = 500, message = "DataNotSaved")
    ])
    @RequestMapping(method = [RequestMethod.POST])
    @ResponseStatus(value = HttpStatus.CREATED)
    fun createSearchRequest(
            @ApiParam("public key owner of search request")
            @PathVariable(value = "owner")
            owner: String,

            @ApiParam("where client sends SearchRequest and signature of the message.", required = true)
            @RequestBody
            request: SignedRequest<SearchRequest>,

            @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
            @RequestHeader("Strategy", required = false)
            strategy: String?): CompletableFuture<SearchRequest> {

        return accountService.accountBySigMessage(request, getStrategyType(strategy))
                .thenCompose { account: Account -> accountService.validateNonce(request, account) }
                .thenCompose {
                    if (owner != it.publicKey) {
                        throw AccessDeniedException()
                    }

                    val result = searchRequestService.createSearchRequest(
                            it.publicKey,
                            request.data!!,
                            getStrategyType(strategy)
                    ).get()

                    accountService.incrementNonce(it, getStrategyType(strategy)).get()

                    CompletableFuture.completedFuture(result)
                }
    }

    /**
     * Delete a search request from the system.
     * The API will verify that the request is cryptographically signed by the owner of the public key.
     * @param request is {@link SignedRequest} where client sends {@link Long} and
     * signature of the message.
     *
     * @return {@link id}, Http status - 200.
     *
     * @exception   {@link BadArgumentException} - 400
     *              {@link AccessDeniedException} - 403
     *              {@link NotFoundException} - 500
     */
    @ApiOperation("Delete a search request from the system.\n" +
            "The API will verify that the request is cryptographically signed by the owner of the public key.",
            response = Long::class)
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Deleted", response = Long::class),
        ApiResponse(code = 400, message = "BadArgumentException"),
        ApiResponse(code = 403, message = "AccessDeniedException"),
        ApiResponse(code = 404, message = "NotFoundException")
    ])
    @RequestMapping(method = [RequestMethod.DELETE], value = ["{id}"])
    fun deleteSearchRequest(
            @ApiParam("id of existed search request.")
            @PathVariable(value = "id")
            id: Long,

            @ApiParam("public key owner of search request")
            @PathVariable(value = "owner")
            owner: String,

            @ApiParam("where client sends SearchRequest id and signature of the message.", required = true)
            @RequestBody
            request: SignedRequest<Long>,

            @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
            @RequestHeader("Strategy", required = false)
            strategy: String?): CompletableFuture<Long> {

        return accountService.accountBySigMessage(request, getStrategyType(strategy))
                .thenCompose { account: Account -> accountService.validateNonce(request, account) }
                .thenCompose {
                    if (request.pk != owner) {
                        throw AccessDeniedException()
                    }

                    if (id != request.data) {
                        throw BadArgumentException()
                    }

                    val result = searchRequestService.deleteSearchRequest(
                            id,
                            owner,
                            getStrategyType(strategy)
                    ).get()
                    accountService.incrementNonce(it, getStrategyType(strategy)).get()

                    CompletableFuture.completedFuture(result)
                }
    }

    /**
     * Return list of existed search requests by owner (Public key of creator).
     *
     * @return {@link List<SearchRequest>}, Http status - 200.
     */
    @ApiOperation(
            "get existed search requests",
            response = SearchRequest::class,
            responseContainer = "List"
    )
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Success", response = List::class)
    ])
    @RequestMapping(method = [RequestMethod.GET], value = ["/", "{id}"])
    fun getSearchRequests(
            @ApiParam("owner who create search requests")
            @PathVariable("owner")
            owner: String,

            @ApiParam("Optional id of existed search request")
            @PathVariable(value = "id", required = false)
            id: Long?,

            @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
            @RequestHeader("Strategy", required = false)
            strategy: String?): CompletableFuture<List<SearchRequest>> {

        return searchRequestService.getSearchRequests(id ?: 0, owner, getStrategyType(strategy))
    }

}
