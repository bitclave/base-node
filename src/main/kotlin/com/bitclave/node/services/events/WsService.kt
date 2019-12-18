package com.bitclave.node.services.events

import com.bitclave.node.utils.Logger
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
class WsService(
    private val messagingTemplate: SimpMessagingTemplate
) {

    fun sendEvent(event: Event, payload: Any): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            messagingTemplate.convertAndSend(event.topic, payload)
        }.exceptionally { e ->
            Logger.error("WsService -> sendEvent: ${e.message}", e)
            throw e
        }
    }
}
