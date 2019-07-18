package com.bitclave.node.utils

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.function.Supplier

val FIXED_THREAD_POOL: ExecutorService = Executors.newFixedThreadPool(10)

fun <T> supplyAsyncEx(supplier: Supplier<T>): CompletableFuture<T> =
    CompletableFuture.supplyAsync(supplier, FIXED_THREAD_POOL)

fun runAsyncEx(runnable: Runnable): CompletableFuture<Void> =
    CompletableFuture.runAsync(runnable, FIXED_THREAD_POOL)
