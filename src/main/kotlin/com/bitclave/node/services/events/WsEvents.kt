package com.bitclave.node.services.events

interface Event {
    val topic: String
}

sealed class OfferEvent(override val topic: String) : Event {
    object OnCreate : OfferEvent("/topic/offer/create")
    object OnUpdate : OfferEvent("/topic/offer/update")
    object OnDelete : OfferEvent("/topic/offer/delete")
}
