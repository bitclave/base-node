package com.bitclave.node

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class BaseNodeApplication

fun main(args: Array<String>) {
    SpringApplication.run(BaseNodeApplication::class.java, *args)
}
