package com.bitclave.node.controllers

import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.CompletableFuture

@RestController
@RequestMapping("/")
class VersionController {

    @Value("\${app.build.version}")
    private val version: String = "0.0.0"

    @RequestMapping(method = [RequestMethod.GET], value = ["version", "ver"])
    fun getVersion(): CompletableFuture<String> = CompletableFuture.completedFuture(version)
}
