package no.nav.henvendelse.config

import no.nav.common.health.selftest.SelfTestCheck
import no.nav.common.log.LogFilter
import no.nav.common.rest.filter.SetStandardHttpHeadersFilter
import no.nav.common.utils.EnvironmentUtils.isDevelopment
import no.nav.henvendelse.APPLICATION_NAME
import no.nav.henvendelse.common.TruststoreCheck
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ApplicationConfig {

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
