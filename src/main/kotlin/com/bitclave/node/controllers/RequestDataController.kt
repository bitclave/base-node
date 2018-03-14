package com.bitclave.node.controllers

import com.bitclave.node.repository.models.Account
import com.bitclave.node.repository.models.RequestData
import com.bitclave.node.repository.models.SignedRequest
import com.bitclave.node.services.AccountService
import com.bitclave.node.services.RequestDataService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.concurrent.CompletableFuture

@RestController
@RequestMapping("/data/")
class RequestDataController(private val accountService: AccountService,
                            private val requestDataService: RequestDataService) :
        AbstractController() {

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
    @RequestMapping(method = [RequestMethod.GET],
            value = [
                "request/from/{fromPk}/state/{state}/",
                "request/to/{toPk}/state/{state}/",
                "request/from/{fromPk}/to/{toPk}/state/{state}/"
            ])
    fun getRequestByState(
            @ApiParam("Optional if use toPk. Public key of the user " +
                    "that issued data access request.", required = false)
            @PathVariable("fromPk", required = false)
            fromPk: String?,

            @ApiParam("Optional if use fromPk. Public key of the user that is expected to\n" +
                    "approve data access request to his personal data.", required = false)
            @PathVariable("toPk", required = false)
            toPk: String?,

            @ApiParam("state of request", required = true)
            @PathVariable("state")
            state: RequestData.RequestDataState,

            @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
            @RequestHeader("Strategy", required = false)
            strategy: String?
    ): CompletableFuture<List<RequestData>> {

        return requestDataService.getRequestByStatus(fromPk, toPk, state, getStrategyType(strategy))
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
                .thenCompose { account: Account ->
                    requestDataService.request(
                            account.publicKey,
                            request.data!!,
                            getStrategyType(strategy)
                    )
                }
    }

    /**
     * Creates a response to a previously submitted data access request.
     * @param id of request
     * @param {@link SignedRequest} with encrypted data
     *
     * @return state of request {@link RequestData.RequestDataState}
     *
     * @exception   {@link BadArgumentException} - 400
     *              {@link AccessDeniedException} - 403
     *              {@link NotFoundException} - 404
     *              {@link DataNotSaved} - 500
     */

    @ApiOperation("Creates a response to a previously submitted data access request.",
            response = RequestData.RequestDataState::class)
    @ApiResponses(value = [
        ApiResponse(code = 200, message = "Success", response = RequestData.RequestDataState::class),
        ApiResponse(code = 400, message = "BadArgumentException"),
        ApiResponse(code = 403, message = "AccessDeniedException"),
        ApiResponse(code = 404, message = "NotFoundException"),
        ApiResponse(code = 500, message = "DataNotSaved")
    ])
    @RequestMapping(method = [RequestMethod.PATCH], value = ["request/{id}/"])
    fun response(
            @ApiParam("id of request", required = true)
            @PathVariable("id")
            requestId: Long,

            @ApiParam("SignedRequest with encrypted data", required = true)
            @RequestBody request: SignedRequest<String>,

            @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
            @RequestHeader("Strategy", required = false)
            strategy: String?
    ): CompletableFuture<RequestData.RequestDataState> {

        return accountService.accountBySigMessage(request, getStrategyType(strategy))
                .thenCompose { account: Account ->
                    requestDataService.response(
                            requestId,
                            account.publicKey,
                            request.data,
                            getStrategyType(strategy)
                    )
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
                .thenCompose { account: Account ->
                    requestDataService.grantAccess(
                            account.publicKey,
                            request.data!!,
                            getStrategyType(strategy)
                    )
                }
    }

}
