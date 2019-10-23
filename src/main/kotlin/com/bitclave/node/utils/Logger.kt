package com.bitclave.node.utils

import org.slf4j.LoggerFactory

// see src/main/resources/logback-spring.xml
enum class LoggerType() {
    PROFILING,
    COMMON
}

class Logger {
    companion object {
        private val loggers = mapOf(
            LoggerType.PROFILING to LoggerFactory.getLogger(LoggerType.PROFILING.name),
            LoggerType.COMMON to LoggerFactory.getLogger(LoggerType.COMMON.name)
        )

        fun debug(message: String, type: LoggerType = LoggerType.COMMON) {
            loggers[type]?.debug(message) ?: loggers[LoggerType.COMMON]?.debug(message)
        }

        fun error(message: String, throwable: Throwable, type: LoggerType = LoggerType.COMMON) {
            loggers[type]?.error(message, throwable) ?: loggers[LoggerType.COMMON]?.error(message, throwable)
        }

        fun info(message: String, type: LoggerType = LoggerType.COMMON) {
            loggers[type]?.info(message) ?: loggers[LoggerType.COMMON]?.info(message)
        }
    }
}
