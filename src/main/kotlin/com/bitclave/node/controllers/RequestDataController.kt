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

    @RequestMapping(method = [RequestMethod.GET],
            value = [
                "from/{fromPk}/state/{state}/",
                "to/{toPk}/state/{state}/",
                "from/{fromPk}/to/{toPk}/state/{state}/"
            ])
    fun getRequestByState(
            @PathVariable("fromPk", required = false) fromPk: String?,
            @PathVariable("toPk", required = false) toPk: String?,
            @PathVariable("state") state: RequestData.RequestDataState)
            : CompletableFuture<List<RequestData>> {

        return requestDataService.getRequestByStatus(fromPk, toPk, state)
    }

    @RequestMapping(method = [RequestMethod.POST])
    fun request(@RequestBody request: SignedRequest<RequestData>): CompletableFuture<Long> {
        return accountService.accountBySigMessage(request)
                .thenCompose { account: Account ->
                    requestDataService.request(account.publicKey, request.data!!)
                }
    }

    @RequestMapping(method = [RequestMethod.PATCH], value = ["{id}/"])
    fun response(@PathVariable("id") requestId: Long,
                 @RequestBody request: SignedRequest<String>): CompletableFuture<RequestData.RequestDataState> {
        return accountService.accountBySigMessage(request)
                .thenCompose { account: Account ->
                    requestDataService.response(requestId, account.publicKey, request.data)
                }
    }

}
