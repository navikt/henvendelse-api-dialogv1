package no.nav.henvendelse.utils.unleash

import no.finn.unleash.strategy.Strategy
import no.nav.common.utils.EnvironmentUtils

class ByEnvironmentStrategy : Strategy {
    val ENVIRONMENT_PROPERTY = "APP_ENVIRONMENT_NAME"
    override fun getName() = "byEnvironment"

    override fun isEnabled(parameters: MutableMap<String, String>?): Boolean {
        val miljo = (parameters?.get("miljø") ?: "").split(",")
        return miljo.any(::isCurrentEnvironment)
    }

    private fun isCurrentEnvironment(env: String): Boolean {
        return EnvironmentUtils.getOptionalProperty(ENVIRONMENT_PROPERTY).orElse("local") == env
    }
}
