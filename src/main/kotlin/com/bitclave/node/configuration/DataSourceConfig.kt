package com.bitclave.node.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import javax.sql.DataSource
import java.util.HashMap
import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.HikariConfig
import com.bitclave.node.routingdatasource.RoutingDataSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment

/**
 * Replication DataSources Configuration
 *
 * `@Primary` and `@DependsOn` are the key requirements for Spring Boot.
 */
@Configuration
class DataSourceConfig {
    private val PRIMARY_DATASOURCE_PREFIX = "spring.primary.datasource"
    private val REPLICA_DATASOURCE_PREFIX = "spring.replica.datasource"

    @Autowired
    private val environment: Environment? = null

    @Bean
    @Primary
    fun dataSource(): DataSource {
        val routingDataSource = RoutingDataSource()

        val primaryDataSource = buildDataSource("PrimaryHikariPool", PRIMARY_DATASOURCE_PREFIX)
        val replicaDataSource = buildDataSource("ReplicaHikariPool", REPLICA_DATASOURCE_PREFIX)

        val targetDataSources = HashMap<Any, Any>()
        targetDataSources[RoutingDataSource.Route.PRIMARY] = primaryDataSource
        targetDataSources[RoutingDataSource.Route.REPLICA] = replicaDataSource

        routingDataSource.setTargetDataSources(targetDataSources)
        routingDataSource.setDefaultTargetDataSource(primaryDataSource)

        return routingDataSource
    }

    private fun buildDataSource(poolName: String, dataSourcePrefix: String): DataSource {
        val hikariConfig = HikariConfig()

        hikariConfig.poolName = poolName
        hikariConfig.jdbcUrl = environment!!.getProperty(String.format("%s.url", dataSourcePrefix))
        hikariConfig.username = environment.getProperty(String.format("%s.username", dataSourcePrefix))
        hikariConfig.password = environment.getProperty(String.format("%s.password", dataSourcePrefix))
        hikariConfig.driverClassName = environment.getProperty(String.format("%s.driver", dataSourcePrefix))

        return HikariDataSource(hikariConfig)
    }
}
