package no.nav.henvendelse.utils

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

class JacksonUtils {
    companion object {
        val objectMapper = jacksonObjectMapper()
            .apply {
                registerModule(JavaTimeModule())
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                setDefaultPrettyPrinter(
                    DefaultPrettyPrinter().apply {
                        indentObjectsWith(DefaultIndenter("  ", "\n"))
                    }
                )
                enable(SerializationFeature.INDENT_OUTPUT)
            }
    }
}
