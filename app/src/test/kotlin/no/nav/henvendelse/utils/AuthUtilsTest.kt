package no.nav.henvendelse.utils

import assertk.Assert
import assertk.assertThat
import assertk.assertions.*
import no.nav.common.auth.subject.IdentType
import no.nav.common.auth.subject.SsoToken
import no.nav.common.auth.subject.Subject
import no.nav.common.auth.subject.SubjectHandler
import no.nav.henvendelse.utils.TestUtils.withProperty
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import kotlin.reflect.KClass

internal class AuthUtilsTest {
    @Test
    fun `kaster ikke feil om operasjon forsøkes i preprod`() {
        withProperty("NAIS_CLUSTER_NAME", "dev-fss") {
            assertThat { AuthUtils.assertNotProd() }.isSuccess()
        }
    }

    @Test
    fun `kaster feil om operasjon forsøkes i produksjon`() {
        withProperty("NAIS_CLUSTER_NAME", "prod-fss") {
            assertThat { AuthUtils.assertNotProd() }
                    .isFailure()
                    .hasType(ResponseStatusException::class)
                    .hasStatus(HttpStatus.FORBIDDEN)
        }
    }

    @Test
    fun `kaster feil dersom ident ikke er satt`() {
        assertThat { AuthUtils.assertAccess() }
                .isFailure()
                .hasType(ResponseStatusException::class)
                .hasStatus(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `kaster feil når identtype er internbruker med ikke godkjent consumerId`() {
        val subject = createSubject(
                ident = "Z999999",
                identType = IdentType.InternBruker,
                consumerId = "dummy"
        )
        SubjectHandler.withSubject(subject) {
            assertThat { AuthUtils.assertAccess() }
                    .isFailure()
                    .hasType(ResponseStatusException::class)
                    .hasStatus(HttpStatus.FORBIDDEN)
        }
    }

    @Test
    fun `kaster feil når identtype er eksternbruker`() {
        val subject = createSubject(
                ident = "Z999999",
                identType = IdentType.EksternBruker,
                consumerId = "dummy"
        )
        SubjectHandler.withSubject(subject) {
            assertThat { AuthUtils.assertAccess() }
                    .isFailure()
                    .hasType(ResponseStatusException::class)
                    .hasStatus(HttpStatus.FORBIDDEN)
        }
    }

    @Test
    fun `kaster ikke feil når identtype er systembruker og systemressurs er godkjent`() {
        val subject = createSubject(
                ident = "srvhenvendelsedialog",
                identType = IdentType.Systemressurs,
                consumerId = "srvhenvendelsedialog"
        )
        SubjectHandler.withSubject(subject) {
            assertThat { AuthUtils.assertAccess() }
                    .isSuccess()
        }
    }

    @Test
    fun `kaster ikke feil når identtype er internbruker og consumerId er godkjent`() {
        val subject = createSubject(
                ident = "Z999999",
                identType = IdentType.InternBruker,
                consumerId = "srvGosys"
        )
        SubjectHandler.withSubject(subject) {
            assertThat { AuthUtils.assertAccess() }
                    .isSuccess()
        }
    }

    @Test
    fun `kaster feil når identtype er systembruker og systemressurs ikke er godkjent`() {
        val subject = createSubject(
                ident = "srvGosys",
                identType = IdentType.Systemressurs,
                consumerId = "srvGosys"
        )
        SubjectHandler.withSubject(subject) {
            assertThat { AuthUtils.assertAccess() }
                    .isFailure()
                    .hasType(ResponseStatusException::class)
                    .hasStatus(HttpStatus.FORBIDDEN)
        }
    }

    fun createSubject(
            ident: String,
            identType: IdentType,
            consumerId: String = "srvGosys"
    ) = Subject(
            ident,
            identType,
            SsoToken.saml(
                    "123",
                    mapOf(
                            "authenticationLevel" to 4,
                            "consumerId" to consumerId
                    )
            )
    )

    fun <T : Any> Assert<Any>.hasType(cls: KClass<T>): Assert<T> {
        this.hasClass(cls)
        return this as Assert<T>
    }

    fun Assert<ResponseStatusException>.hasStatus(status: HttpStatus) {
        prop("status") { it.status }.isEqualTo(status)
    }
}
