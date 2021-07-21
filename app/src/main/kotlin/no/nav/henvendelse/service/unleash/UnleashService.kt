package no.nav.henvendelse.service.unleash

import no.finn.unleash.DefaultUnleash
import no.finn.unleash.Unleash
import no.finn.unleash.UnleashContext
import no.finn.unleash.event.UnleashSubscriber
import no.finn.unleash.repository.FeatureToggleResponse
import no.finn.unleash.strategy.Strategy
import no.finn.unleash.util.UnleashConfig
import no.nav.common.auth.subject.SsoToken
import no.nav.common.auth.subject.SubjectHandler
import no.nav.common.health.HealthCheck
import no.nav.common.health.HealthCheckResult
import no.nav.common.utils.EnvironmentUtils
import no.nav.henvendelse.APPLICATION_NAME

interface UnleashService : HealthCheck {
    fun isEnabled(toggleName: String): Boolean
    fun isEnabled(toggleName: String, context: UnleashContext): Boolean
}

class UnleashServiceImpl : UnleashService, UnleashSubscriber {
    companion object {
        fun resolveUnleashContext(): UnleashContext {
            val subject: String? = SubjectHandler.getIdent().orElse(null)
            val token: String? = SubjectHandler.getSsoToken().map(SsoToken::getToken).orElse(null)
            return UnleashContext.builder()
                .userId(subject)
                .sessionId(token)
                .build()
        }
    }
    private val unleash: Unleash
    var lastFetchStatus: FeatureToggleResponse.Status? = null

    constructor(unleash: Unleash) {
        this.unleash = unleash
    }

    constructor(vararg strategies: Strategy) {
        val config = UnleashConfig.builder()
            .appName(APPLICATION_NAME)
            .unleashAPI(EnvironmentUtils.getRequiredProperty("UNLEASH_API_URL"))
            .subscriber(this)
            .build()
        this.unleash = DefaultUnleash(config, *strategies)
    }

    override fun isEnabled(toggleName: String): Boolean = unleash.isEnabled(toggleName, resolveUnleashContext())
    override fun isEnabled(toggleName: String, context: UnleashContext): Boolean = unleash.isEnabled(toggleName, context)

    override fun togglesFetched(toggleResponse: FeatureToggleResponse?) {
        this.lastFetchStatus = toggleResponse?.status
    }

    override fun checkHealth(): HealthCheckResult {
        return when (lastFetchStatus) {
            FeatureToggleResponse.Status.CHANGED -> HealthCheckResult.healthy()
            FeatureToggleResponse.Status.NOT_CHANGED -> HealthCheckResult.healthy()
            FeatureToggleResponse.Status.UNAVAILABLE -> HealthCheckResult.unhealthy(lastFetchStatus?.toString())
            else -> HealthCheckResult.unhealthy("No status found")
        }
    }
}
