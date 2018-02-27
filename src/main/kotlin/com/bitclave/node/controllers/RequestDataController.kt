package com.bitclave.node.controllers

import com.bitclave.node.repository.models.Account
import com.bitclave.node.repository.models.RequestData
import com.bitclave.node.repository.models.SignedRequest
import com.bitclave.node.services.AccountService
import com.bitclave.node.services.RequestDataService
import org.springframework.web.bind.annotation.*
import java.util.concurrent.CompletableFuture

@RestController
@RequestMapping("/request/")
class RequestDataController(private val accountService: AccountService,
                            private val requestDataService: RequestDataService) {

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
    @RequestMapping(method = [RequestMethod.GET],
            value = [
                "from/{fromPk}/state/{state}/",
                "to/{toPk}/state/{state}/",
                "from/{fromPk}/to/{toPk}/state/{state}/"
            ])
    fun getRequestByState(
            @PathVariable("fromPk", required = false) fromPk: String?,
            @PathVariable("toPk", required = false) toPk: String?,
            @PathVariable("state") state: RequestData.RequestDataState
    ): CompletableFuture<List<RequestData>> {

        return requestDataService.getRequestByStatus(fromPk, toPk, state)
    }

    /**
     * Create request for get private client data.
     * @param {@link SignedRequest} with {@link RequestData}.
     *
     * @return id of created request.
     *
     * @exception   {@link AccessDeniedException} - 403
     *              {@link NotFoundException} - 404
     *              {@link BadArgumentException} - 400
     *              {@link DataNotSaved} - 500
     */
    @RequestMapping(method = [RequestMethod.POST])
    fun request(@RequestBody request: SignedRequest<RequestData>): CompletableFuture<Long> {
        return accountService.accountBySigMessage(request)
                .thenCompose { account: Account ->
                    requestDataService.request(account.publicKey, request.data!!)
                }
    }

    /**
     * Creates data access request to a specific user for a specific personal data.
     * @param {@link SignedRequest} with {@link RequestData}.
     *
     * @return id of the created data access request.
     *
     * @exception   {@link AccessDeniedException} - 403
     *              {@link NotFoundException} - 404
     *              {@link BadArgumentException} - 400
     *              {@link DataNotSaved} - 500
     */
    @RequestMapping(method = [RequestMethod.PATCH], value = ["{id}/"])
    fun response(
            @PathVariable("id") requestId: Long,
            @RequestBody request: SignedRequest<String>
    ): CompletableFuture<RequestData.RequestDataState> {

        return accountService.accountBySigMessage(request)
                .thenCompose { account: Account ->
                    requestDataService.response(requestId, account.publicKey, request.data)
                }
    }

}
