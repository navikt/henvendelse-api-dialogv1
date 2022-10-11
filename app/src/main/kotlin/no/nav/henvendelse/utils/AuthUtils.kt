package no.nav.henvendelse.utils

import no.nav.common.auth.subject.IdentType
import no.nav.common.auth.subject.SsoToken
import no.nav.common.auth.subject.SubjectHandler
import no.nav.common.utils.EnvironmentUtils
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.util.*

object AuthUtils {
    private val tjenestekallLogg = LoggerFactory.getLogger("SecureLog")
    private val validConsumerIds = listOf("srvgosys", "srvpensjon", "srvbisys")
    private val validSystemResource = listOf("srvhenvendelsedialog", "srvgosys", "srvpensjon", "srvbisys")

    data class Subject(val subject: String, val type: IdentType)

    fun assertAccess(): Subject {
        val ident: String? = SubjectHandler.getIdent().orElse(null)
        val identtype: IdentType? = SubjectHandler.getIdentType().orElse(null)
        val consumerId: String? = SubjectHandler.getSsoToken().getAttribute("consumerId")

        if (ident == null || identtype == null) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "API kan ikke kalles uten innlogget bruker, men ble kalt med '$ident'")
        } else if (identtype == IdentType.EksternBruker) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "API kan ikke brukes av EksternBrukere, men ble kalt med '$identtype'")
        } else if (identtype == IdentType.InternBruker && !isValidConsumerId(consumerId)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "API kan bare brukes via godkjente systemer, men ble kalt via '$consumerId'")
        } else if (identtype == IdentType.Systemressurs && !isValidSystemResource(ident)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "API kan bare brukes av godkjente systemer, men ble kalt av '$ident'")
        }
        return Subject(ident, identtype)
    }

    fun ifInternUser(block: (ident: String) -> Unit) {
        try {
            val (ident, identtype) = assertAccess()
            if (identtype == IdentType.InternBruker) {
                block(ident)
            }
        } catch (_: Throwable) {}
    }

    fun assertNotProd() {
        val assumedProd = EnvironmentUtils.isProduction().orElse(true)
        if (assumedProd) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Operasjon kan bare testes i preprod")
        }
    }

    private fun isValidConsumerId(consumerId: String?) = validConsumerIds.contains(consumerId?.lowercase())

    private fun isValidSystemResource(ident: String?) = validSystemResource.contains(ident?.lowercase())

    private fun Optional<SsoToken>.getAttribute(name: String): String? {
        return this.map { it.attributes }
            .filter { it != null }
            .map { it[name] as String? }
            .orElse(null)
    }
}
