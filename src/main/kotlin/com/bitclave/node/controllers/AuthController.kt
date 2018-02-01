package com.bitclave.node.controllers

import com.bitclave.node.repository.models.Account
import com.bitclave.node.services.AccountService
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.CompletableFuture

@RestController()
@RequestMapping("/")
class AuthController(private val accountService: AccountService) {

    @RequestMapping(method = [RequestMethod.POST], value = ["signUp"])
    fun signUp(@RequestBody account: Account): CompletableFuture<Account> {
        return accountService.registrationClient(account)
    }

    @RequestMapping(method = [RequestMethod.POST], value = ["signIn"])
    fun signIn(@RequestBody account: Account): CompletableFuture<Account> {
        return accountService.authorization(account)
    }

}
