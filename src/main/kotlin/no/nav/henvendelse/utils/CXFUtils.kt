package no.nav.henvendelse.utils

import no.nav.common.health.HealthCheckResult
import no.nav.common.health.selftest.SelfTestCheck

inline fun <reified T> CXFClient() = no.nav.common.cxf.CXFClient(T::class.java)

fun interface Pingable {
    fun ping(): SelfTestCheck
}

fun createPingable(
    description: String,
    critical: Boolean,
    test: () -> Unit
) = Pingable {
    SelfTestCheck(description, critical) {
        try {
            test()
            HealthCheckResult.healthy()
        } catch (throwable: Throwable) {
            HealthCheckResult.unhealthy(throwable)
        }
    }
}
