package no.nav.henvendelse.utils

import assertk.Assert
import assertk.assertThat
import assertk.assertions.*
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

    fun <T : Any> Assert<Any>.hasType(cls: KClass<T>): Assert<T> {
        this.hasClass(cls)
        return this as Assert<T>
    }

    fun Assert<ResponseStatusException>.hasStatus(status: HttpStatus) {
        prop("status") { it.status }.isEqualTo(status)
    }
}