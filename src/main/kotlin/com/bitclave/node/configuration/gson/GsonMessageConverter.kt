package com.bitclave.node.configuration.gson

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.springframework.messaging.Message
import org.springframework.messaging.MessageHeaders
import org.springframework.messaging.converter.AbstractMessageConverter
import org.springframework.util.MimeType
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.StringReader
import java.io.StringWriter
import java.nio.charset.StandardCharsets

class GsonMessageConverter(val gson: Gson) :
    AbstractMessageConverter(MimeType("application", "json", StandardCharsets.UTF_8)) {

    override fun isStrictContentTypeMatch(): Boolean = false

    override fun supports(clazz: Class<*>): Boolean = true

    override fun convertFromInternal(message: Message<*>, targetClass: Class<*>, conversionHint: Any?): Any? {
        val token: TypeToken<*> = TypeToken.get(targetClass)
        val payload = message.payload

        val reader = if (payload is ByteArray) {
            InputStreamReader(
                ByteArrayInputStream(payload),
                getMimeType(message.headers)?.charset ?: StandardCharsets.UTF_8
            )
        } else {
            StringReader(payload as String)
        }

        return gson.fromJson(reader, token.type)
    }

    override fun convertToInternal(payload: Any?, headers: MessageHeaders?, conversionHint: Any?): Any? {
        return if (ByteArray::class.java == serializedPayloadClass) {
            val out = ByteArrayOutputStream(1024)
            val writer = OutputStreamWriter(out, getMimeType(headers)?.charset ?: StandardCharsets.UTF_8)

            gson.toJson(payload, writer)

            writer.close()
            out.toByteArray()
        } else {
            val writer = StringWriter()
            gson.toJson(payload, writer)
            writer.toString()
        }
    }
}
