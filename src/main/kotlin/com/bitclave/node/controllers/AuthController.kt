package com.bitclave.node.controllers

import com.bitclave.node.repository.models.Account
import com.bitclave.node.repository.models.SignedRequest
import com.bitclave.node.services.AccountService
import com.bitclave.node.services.errors.AccessDeniedException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.concurrent.CompletableFuture

@RestController()
@RequestMapping("/")
class AuthController(private val accountService: AccountService) {

    @RequestMapping(method = [RequestMethod.POST], value = ["registration"])
    @ResponseStatus(value = HttpStatus.CREATED)
    fun registration(@RequestBody request: SignedRequest<Account>): CompletableFuture<Account> {
        return accountService.checkSigMessage(request)
                .thenApply { pk ->
                    if (pk != request.data?.publicKey) {
                        throw AccessDeniedException()
                    }
                    pk
                }
                .thenCompose { accountService.registrationClient(request.data!!) }
    }

    @RequestMapping(method = [RequestMethod.POST], value = ["exist"])
    fun existAccount(@RequestBody request: SignedRequest<Account>): CompletableFuture<Account> {
        return accountService.checkSigMessage(request)
                .thenApply { pk ->
                    if (pk != request.data?.publicKey) {
                        throw AccessDeniedException()
                    }
                    pk
                }
                .thenCompose { accountService.existAccount(request.data!!) }
    }

}
