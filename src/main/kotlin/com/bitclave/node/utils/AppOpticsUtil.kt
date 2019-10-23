package com.bitclave.node.utils

import com.appoptics.metrics.client.AppopticsClient
import com.appoptics.metrics.client.Duration
import com.appoptics.metrics.client.Measure
import com.appoptics.metrics.client.Measures
import com.appoptics.metrics.client.PostMeasuresResult
import com.appoptics.metrics.client.PostResult
import com.appoptics.metrics.client.Tag
import com.bitclave.node.configuration.properties.AppOpticsProperties
import java.util.concurrent.TimeUnit

class AppOpticsUtil(
    appOpticsProperties: AppOpticsProperties
) {

    private val client = AppopticsClient.builder(appOpticsProperties.serviceKey)
        // these are optional
        .setConnectTimeout(Duration(5, TimeUnit.SECONDS))
        .setReadTimeout(Duration(5, TimeUnit.SECONDS))
        .setAgentIdentifier("base-node")
        // and finally build
        .build()

    fun sendToAppOptics(
        name: String,
        value: Double,
        vararg tags: Tag
    ) {
        val result: PostMeasuresResult = client.postMeasures(Measures().add(Measure(name, value, *tags)))

        for (postResult: PostResult in result.results) {
            if (postResult.isError) {
                Logger.debug(postResult.toString())
            }
        }
    }
}
