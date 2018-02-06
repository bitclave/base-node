package com.bitclave.node.controllers

import com.bitclave.node.services.ClientDataService
import org.springframework.web.bind.annotation.*
import java.util.concurrent.CompletableFuture

@RestController
@RequestMapping("/client/{id}/")
class ClientDataController(private val clientDataService: ClientDataService) {

    @RequestMapping(method = [RequestMethod.GET])
    fun getData(@PathVariable("id") clientId: String): CompletableFuture<Map<String, String>> {
        return clientDataService.getData(clientId)
    }

    @RequestMapping(method = [RequestMethod.PATCH])
    fun updateData(@PathVariable("id") clientId: String, @RequestBody
    data: Map<String, String>): CompletableFuture<Map<String, String>> {
        return clientDataService.updateData(clientId, data)
    }

}
