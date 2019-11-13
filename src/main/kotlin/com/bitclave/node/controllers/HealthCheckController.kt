package com.bitclave.node.controllers

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.CompletableFuture

@RestController
@RequestMapping("/")
class HealthCheckController {
    // don't change this route path. it used in "k8s/staging/service-staging.yml"
    @RequestMapping(method = [RequestMethod.GET], value = ["health-check"])
    fun healthCheck(): CompletableFuture<ResponseEntity<Void>> =
        CompletableFuture.completedFuture(ResponseEntity.ok().build())
}
