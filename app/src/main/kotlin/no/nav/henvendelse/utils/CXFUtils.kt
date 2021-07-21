package no.nav.henvendelse.utils

import no.nav.common.health.HealthCheckResult
import no.nav.common.health.selftest.SelfTestCheck

inline fun <reified T> CXFClient() = no.nav.common.cxf.CXFClient(T::class.java)

fun createSelfTestCheck(
    description: String,
    critical: Boolean,
    test: () -> Unit
) = SelfTestCheck(description, critical) {
    try {
        test()
        HealthCheckResult.healthy()
    } catch (throwable: Throwable) {
        HealthCheckResult.unhealthy(throwable)
    }
}
