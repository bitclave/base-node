package com.bitclave.node.controllers.dev

import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.CompletableFuture

@RestController()
@RequestMapping("/dev/bot/")
class BotController {

    @RequestMapping(method = [RequestMethod.GET], value = ["{name}"])
    fun getPublicKey(@PathVariable("name") name: String): CompletableFuture<String> {
        return CompletableFuture.completedFuture(
                if ("Adam".equals(name, true))
                    "038d4a758b58137ee47993ca434c1b797096536ada167b942f7d251ed1fc50c1c1"
                else
                    ""
        )
    }

}
