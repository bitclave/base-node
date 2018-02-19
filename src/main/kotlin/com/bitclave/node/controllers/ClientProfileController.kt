package com.bitclave.node.controllers

import com.bitclave.node.repository.models.Account
import com.bitclave.node.repository.models.SignedRequest
import com.bitclave.node.services.AccountService
import com.bitclave.node.services.ClientProfileService
import org.springframework.web.bind.annotation.*
import java.util.concurrent.CompletableFuture

@RestController
@RequestMapping("/client/")
class ClientProfileController(private val accountService: AccountService,
                              private val profileService: ClientProfileService) {

    @RequestMapping(method = [RequestMethod.GET], value = ["{pk}/"])
    fun getData(
            @PathVariable("pk") publicKey: String
    ): CompletableFuture<Map<String, String>> {

        return profileService.getData(publicKey)
    }

    @RequestMapping(method = [RequestMethod.PATCH])
    fun updateData(
            @RequestBody request: SignedRequest<Map<String, String>>
    ): CompletableFuture<Map<String, String>> {

        return accountService.accountBySigMessage(request)
                .thenCompose { account: Account ->
                    profileService.updateData(account.publicKey, request.data!!)
                }
    }

}
