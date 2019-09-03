package com.bitclave.node.services.events

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
        }
    }
}
