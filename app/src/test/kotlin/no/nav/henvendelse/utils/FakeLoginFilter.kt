package no.nav.henvendelse.utils

import no.nav.common.auth.subject.IdentType
import no.nav.common.auth.subject.SsoToken
import no.nav.common.auth.subject.Subject
import no.nav.common.auth.subject.SubjectHandler
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse

class FakeLoginFilter : Filter {
    val subject = Subject(
        "Z999999",
        IdentType.InternBruker,
        SsoToken.oidcToken("dummy", mapOf("consumerId" to "srvgosys"))
    )
    override fun doFilter(req: ServletRequest, resp: ServletResponse, chain: FilterChain) {
        SubjectHandler.withSubject(subject) {
            chain.doFilter(req, resp)
        }
    }
}
