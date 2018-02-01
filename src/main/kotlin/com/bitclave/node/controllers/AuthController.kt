package com.bitclave.node.controllers

import com.bitclave.node.repository.models.Account
import com.bitclave.node.services.AccountService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.concurrent.CompletableFuture

@RestController()
@RequestMapping("/")
class AuthController(private val accountService: AccountService) {

    @RequestMapping(method = [RequestMethod.POST], value = ["signUp"])
    @ResponseStatus(value = HttpStatus.CREATED)
    fun signUp(@RequestBody account: Account): CompletableFuture<Account> {
        return accountService.registrationClient(account)
    }

    @RequestMapping(method = [RequestMethod.POST], value = ["signIn"])
    fun signIn(@RequestBody account: Account): CompletableFuture<Account> {
        return accountService.authorization(account)
    }

}
