package com.bitclave.node.configuration

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.LayoutBase
import com.bitclave.node.configuration.gson.GsonConfig
import org.springframework.context.annotation.Configuration
import java.text.SimpleDateFormat
import java.util.Date

data class LogbackElasticLevelData(
    val level: Long,
    val levelStr: String,
    val colour: String = ""
)

data class LogbackElasticContextData(
    val env: String = "",
    val filename: String = "",
    val ip: String = "",
    val layer: String = "",
    val localLabel: String = "",
    val pk: String = "",
    val source: String = "",
    val user: String = ""
)

data class LogbackElasticData(
    val categoryName: String,
    val data: String,
    val startTime: String,
    val context: LogbackElasticContextData,
    val level: LogbackElasticLevelData
)

@Configuration
class LogbackLayout : LayoutBase<ILoggingEvent>() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

    override fun doLayout(event: ILoggingEvent): String {
        val data = LogbackElasticData(
            "base-node",
            event.formattedMessage,
            dateFormat.format(Date(event.timeStamp)),
            LogbackElasticContextData(source = event.threadName, filename = event.loggerName),
            LogbackElasticLevelData(event.level.levelInt.toLong(), event.level.levelStr)
        )

        return GsonConfig.GSON.toJson(data)
    }
}
