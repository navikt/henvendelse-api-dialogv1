package no.nav.henvendelse.config

import no.nav.common.cxf.StsConfig
import no.nav.common.health.HealthCheckResult
import no.nav.common.health.selftest.SelfTestCheck
import no.nav.common.log.LogFilter
import no.nav.common.rest.filter.SetStandardHttpHeadersFilter
import no.nav.common.sts.NaisSystemUserTokenProvider
import no.nav.common.sts.SystemUserTokenProvider
import no.nav.common.utils.EnvironmentUtils.getRequiredProperty
import no.nav.common.utils.EnvironmentUtils.isDevelopment
import no.nav.henvendelse.APPLICATION_NAME
import no.nav.henvendelse.SERVICEUSER_PASSWORD_PROPERTY
import no.nav.henvendelse.SERVICEUSER_USERNAME_PROPERTY
import no.nav.henvendelse.common.TruststoreCheck
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ApplicationConfig {

    @Bean
    fun stsConfig() = StsConfig.builder()
        .url(getRequiredProperty("SECURITY_TOKEN_SERVICE_SAML_URL"))
        .username(getRequiredProperty(SERVICEUSER_USERNAME_PROPERTY))
        .password(getRequiredProperty(SERVICEUSER_PASSWORD_PROPERTY))
        .build()

    @Bean
    fun systemUserTokenProvider(): SystemUserTokenProvider = NaisSystemUserTokenProvider(
        getRequiredProperty("SECURITY_TOKEN_SERVICE_DISCOVERY_URL"),
        getRequiredProperty(SERVICEUSER_USERNAME_PROPERTY),
        getRequiredProperty(SERVICEUSER_PASSWORD_PROPERTY)
    )

    @Bean
    fun systemUserTokenProviderCheck(provider: SystemUserTokenProvider) = SelfTestCheck(
        "SystemUserTokenProvider",
        true
    ) {
        try {
            requireNotNull(provider.systemUserToken)
            HealthCheckResult.healthy()
        } catch (e: Exception) {
            HealthCheckResult.unhealthy(e)
        }
    }

    @Bean
    fun truststoreCheck(): SelfTestCheck = TruststoreCheck.create()

    @Bean
    fun logFilter(): FilterRegistrationBean<LogFilter> {
        return FilterRegistrationBean(LogFilter(APPLICATION_NAME, isDevelopment().orElse(false)))
            .also {
                it.order = 1
                it.urlPatterns = listOf("/*")
            }
    }

    @Bean
    fun setStandardHttpHeadersFilter(): FilterRegistrationBean<SetStandardHttpHeadersFilter> {
        return FilterRegistrationBean(SetStandardHttpHeadersFilter())
            .also {
                it.order = 2
                it.urlPatterns = listOf("/*")
            }
    }
}
