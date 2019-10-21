package com.bitclave.node.configuration

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.LayoutBase
import org.springframework.context.annotation.Configuration
import java.text.SimpleDateFormat
import java.util.Date

@Configuration
class LogbackDrainsLayout : LayoutBase<ILoggingEvent>() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

    override fun doLayout(event: ILoggingEvent): String {
        var message = event.formattedMessage

        if (event.level == Level.ERROR) {
            val errorStackTrace =
                event.throwableProxy.stackTraceElementProxyArray?.contentToString() ?: "[stacktrace undefined]"
            message = message.plus("; stacktrace: ")
            message = message.plus(errorStackTrace)
        }

        return "base-node" +
            " ${dateFormat.format(Date(event.timeStamp))}" +
            " ${event.level.levelStr}" +
            " ${event.threadName}" +
            " ${event.loggerName} $message \n"
    }
}
