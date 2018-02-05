package com.bitclave.node.controllers

import org.springframework.web.bind.annotation.*
import java.util.concurrent.CompletableFuture

@RestController
@RequestMapping("/client/{id}/")
class ClientDataController {

    val result: Map<String, String> = hashMapOf("firstName" to "My First Name",
            "secondName" to "My Second Name")
    val emptyResult: Map<String, String> = HashMap()

    @RequestMapping(method = [RequestMethod.GET])
    fun getData(@PathVariable("id") clientId: String): CompletableFuture<Map<String, String>> {
        return CompletableFuture.completedFuture(if ("first" == clientId) result else emptyResult);
    }

    @RequestMapping(method = [RequestMethod.PATCH])
    fun updateData(@PathVariable("id") clientId: String, @RequestBody
    data: Map<String, String>): CompletableFuture<Map<String, String>> {
        return CompletableFuture.completedFuture(data)
    }

}
