package no.nav.henvendelse.utils

import no.nav.common.auth.subject.IdentType
import no.nav.common.auth.subject.SubjectHandler
import no.nav.common.utils.EnvironmentUtils
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

object AuthUtils {
    data class Subject(val subject: String, val type: IdentType)

    fun assertAccess(): Subject {
        val ident: String? = SubjectHandler.getIdent().orElse(null)
        val identtype: IdentType? = SubjectHandler.getIdentType().orElse(null)

        if (ident == null) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "API kan ikke kalles uten innlogget bruker, men ble kalt med '$ident'")
        } else if (identtype !== IdentType.Systemressurs) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "API kan bare brukes av Systemressurser, men ble kalt med '$identtype'")
        }
        return Subject(ident, identtype)
    }

    fun assertNotProd() {
        val assumedProd = EnvironmentUtils.isProduction().orElse(true)
        if (assumedProd) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Operasjon kan bare teses i preprod")
        }
    }
}