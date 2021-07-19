package no.nav.henvendelse.common

import no.nav.common.health.HealthCheck
import no.nav.common.health.HealthCheckResult
import no.nav.common.health.selftest.SelfTestCheck
import no.nav.common.utils.EnvironmentUtils
import no.nav.common.utils.SslUtils
import no.nav.henvendelse.utils.Pingable

class TruststoreCheck : HealthCheck {
    override fun checkHealth(): HealthCheckResult {
        val truststore = EnvironmentUtils.getOptionalProperty(SslUtils.JAVAX_NET_SSL_TRUST_STORE)
        return truststore
            .map { HealthCheckResult.healthy() }
            .orElseGet { HealthCheckResult.unhealthy(truststore.orElse("N/A")) }
    }
    companion object {
        fun create() = Pingable {
            SelfTestCheck(
                "Sjekker at truststore er satt",
                true,
                TruststoreCheck()
            )
        }
    }
}
