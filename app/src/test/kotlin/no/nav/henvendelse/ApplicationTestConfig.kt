package no.nav.henvendelse

import no.nav.henvendelse.utils.FakeLoginFilter
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
class ApplicationTestConfig {
    @Bean
    @Profile("local")
    fun fakeLoginFilter(): FilterRegistrationBean<FakeLoginFilter> {
        return FilterRegistrationBean(FakeLoginFilter())
            .also {
                it.order = 1
                it.urlPatterns = listOf("/rest/*", "/internal/debug")
            }
    }
}