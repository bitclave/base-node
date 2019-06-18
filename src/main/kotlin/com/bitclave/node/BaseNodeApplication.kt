package com.bitclave.node

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import java.util.concurrent.Executors

@SpringBootApplication
class BaseNodeApplication {
    companion object {
        val FIXED_THREAD_POOL = Executors.newFixedThreadPool(100)
    }
}

fun main(args: Array<String>) {
    SpringApplication.run(BaseNodeApplication::class.java, *args)
}
