package com.bitclave.node.controllers

import com.bitclave.node.repository.models.Account
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.CompletableFuture

@RestController()
@RequestMapping("/")
class AuthController {

    @RequestMapping(method = [RequestMethod.POST], value = ["signUp"])
    fun signUp(): CompletableFuture<Account> {
        return CompletableFuture.completedFuture(
                Account("is some user id"))
    }

    @RequestMapping(method = [RequestMethod.POST], value = ["signIn"])
    fun signIn(): CompletableFuture<Account> {
        return CompletableFuture.completedFuture(
                Account("is same user id"))
    }

}
