package com.bitclave.node.controllers.v1

import com.bitclave.node.controllers.AbstractController
import com.bitclave.node.repository.models.Account
import com.bitclave.node.repository.models.RequestData
import com.bitclave.node.repository.models.SignedRequest
import com.bitclave.node.services.v1.AccountService
import com.bitclave.node.services.v1.RequestDataService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.concurrent.CompletableFuture

@RestController
@RequestMapping("/v1/data/")
class RequestDataController(
        @Qualifier("v1") private val accountService: AccountService,
        @Qualifier("v1") private val requestDataService: RequestDataService
) : AbstractController() {

    /**
     * Returns a list of outstanding data access requests,
     * where data access requests meet the provided search criteria.
     * API called must provided one of fromPk or toPk.
     * @param fromPk - Optional public key of the user that issued data access request.
     * @param toPk - Optional public key of the user that is expected to
     * approve data access request to his personal data.
     * @param state - {@link RequestData.RequestDataState}.
     *
     * @return List of {@link RequestData}, or empty list. Http status - 200.
     *
     * @exception {@link BadArgumentException} - 400
     */
    @ApiOperation("Returns a list of outstanding data access requests,\n" +
            "where data access requests meet the provided search criteria.\n" +
            "API called must provided one of fromPk or toPk.",
            response = RequestData::class, responseContainer = "List")
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Success", response = RequestData::class,
                responseContainer = "List"),
        ApiResponse(code = 400, message = "BadArgumentException")
    ])
    @RequestMapping(method = [RequestMethod.GET], value = ["request/"])
    fun getRequestByState(
            @ApiParam("Optional if use toPk. Public key of the user " +
                    "that issued data access request.", required = false)
            @RequestParam("fromPk", required = false)
            fromPk: String?,

            @ApiParam("Optional if use fromPk. Public key of the user that is expected to\n" +
                    "approve data access request to his personal data.", required = false)
            @RequestParam("toPk", required = false)
            toPk: String?,

            @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
            @RequestHeader("Strategy", required = false)
            strategy: String?
    ): CompletableFuture<List<RequestData>> {

        return requestDataService.getRequestByStatus(fromPk, toPk, getStrategyType(strategy))
    }

    /**
     * Create request for get private client data.
     * @param request info of request for privacy client data
     *
     * @return id of created request.
     *
     * @exception   {@link BadArgumentException} - 400
     *              {@link AccessDeniedException} - 403
     *              {@link NotFoundException} - 404
     *              {@link DataNotSaved} - 500
     */
    @ApiOperation("Create request for get private client data.", response = Long::class)
    @ApiResponses(value = [
        ApiResponse(code = 201, message = "Created", response = Long::class),
        ApiResponse(code = 400, message = "BadArgumentException"),
        ApiResponse(code = 403, message = "AccessDeniedException"),
        ApiResponse(code = 404, message = "NotFoundException"),
        ApiResponse(code = 500, message = "DataNotSaved")
    ])
    @RequestMapping(method = [RequestMethod.POST], value = ["request/"])
    @ResponseStatus(HttpStatus.CREATED)
    fun request(
            @ApiParam("info of request for privacy client data", required = true)
            @RequestBody
            request: SignedRequest<RequestData>,

            @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
            @RequestHeader("Strategy", required = false)
            strategy: String?
    ): CompletableFuture<Long> {

        return accountService.accountBySigMessage(request, getStrategyType(strategy))
                .thenCompose { account: Account -> accountService.validateNonce(request, account) }
                .thenCompose { account: Account ->
                    val result = requestDataService.request(
                            account.publicKey,
                            request.data!!,
                            getStrategyType(strategy)
                    ).get()

                    accountService.incrementNonce(account, getStrategyType(strategy)).get()

                    CompletableFuture.completedFuture(result)
                }
    }

    /**
     * Grant access for get private client data.
     * @param request info of request for privacy client data
     *
     * @return id of created request.
     *
     * @exception   {@link BadArgumentException} - 400
     *              {@link AccessDeniedException} - 403
     *              {@link NotFoundException} - 404
     *              {@link DataNotSaved} - 500
     */
    @ApiOperation("Grant access for get private client data.", response = Long::class)
    @ApiResponses(value = [
        ApiResponse(code = 201, message = "Created", response = Long::class),
        ApiResponse(code = 400, message = "BadArgumentException"),
        ApiResponse(code = 403, message = "AccessDeniedException"),
        ApiResponse(code = 404, message = "NotFoundException"),
        ApiResponse(code = 500, message = "DataNotSaved")
    ])
    @RequestMapping(method = [RequestMethod.POST], value = ["grant/request/"])
    @ResponseStatus(HttpStatus.CREATED)
    fun grantAccess(
            @ApiParam("info of request for privacy client data", required = true)
            @RequestBody
            request: SignedRequest<RequestData>,

            @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
            @RequestHeader("Strategy", required = false)
            strategy: String?
    ): CompletableFuture<Long> {

        return accountService.accountBySigMessage(request, getStrategyType(strategy))
                .thenCompose { account: Account -> accountService.validateNonce(request, account) }
                .thenCompose { account: Account ->
                    val result = requestDataService.grantAccess(
                            account.publicKey,
                            request.data!!,
                            getStrategyType(strategy)
                    ).get()

                    accountService.incrementNonce(account, getStrategyType(strategy)).get()

                    CompletableFuture.completedFuture(result)
                }
    }

}
