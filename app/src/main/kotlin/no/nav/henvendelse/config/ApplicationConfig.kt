package no.nav.henvendelse.config

import no.nav.common.auth.oidc.filter.OidcAuthenticationFilter
import no.nav.common.auth.oidc.filter.OidcAuthenticator
import no.nav.common.auth.oidc.filter.OidcAuthenticatorConfig
import no.nav.common.auth.subject.IdentType
import no.nav.common.health.selftest.SelfTestCheck
import no.nav.common.log.LogFilter
import no.nav.common.rest.filter.SetStandardHttpHeadersFilter
import no.nav.common.utils.EnvironmentUtils
import no.nav.common.utils.EnvironmentUtils.isDevelopment
import no.nav.henvendelse.APPLICATION_NAME
import no.nav.henvendelse.common.TruststoreCheck
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
class ApplicationConfig {

    @Bean
    fun truststoreCheck(): SelfTestCheck = TruststoreCheck.create()

    @Bean
    @Profile("!local")
    fun oidcLoginFilter(): FilterRegistrationBean<OidcAuthenticationFilter> {
        val config = OidcAuthenticator.fromConfigs(
            OidcAuthenticatorConfig()
                .withClientId(EnvironmentUtils.getRequiredProperty("AZURE_APP_CLIENT_ID"))
                .withDiscoveryUrl(EnvironmentUtils.getRequiredProperty("AZURE_APP_WELL_KNOWN_URL"))
                .withIdentType(IdentType.InternBruker)
        )

        return FilterRegistrationBean(OidcAuthenticationFilter(config))
            .also {
                it.order = 1
                it.urlPatterns = listOf("/rest/*", "/internal/debug")
            }
    }

    @Bean
    fun logFilter(): FilterRegistrationBean<LogFilter> {
        return FilterRegistrationBean(LogFilter(APPLICATION_NAME, isDevelopment().orElse(false)))
            .also {
                it.order = 2
                it.urlPatterns = listOf("/*")
            }
    }

    @Bean
    fun setStandardHttpHeadersFilter(): FilterRegistrationBean<SetStandardHttpHeadersFilter> {
        return FilterRegistrationBean(SetStandardHttpHeadersFilter())
            .also {
                it.order = 3
                it.urlPatterns = listOf("/*")
            }
    }
}
