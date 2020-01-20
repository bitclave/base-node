package com.bitclave.node.routingdatasource

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource

class RoutingDataSource : AbstractRoutingDataSource() {

    enum class Route {
        PRIMARY, REPLICA
    }

    companion object {
        private val routeContext = ThreadLocal<Route>()
        fun clearReplicaRoute() {
            routeContext.remove()
        }

        fun setReplicaRoute() {
            routeContext.set(Route.REPLICA)
        }
    }

    override fun determineCurrentLookupKey(): Any? {
        return routeContext.get()
    }
}
