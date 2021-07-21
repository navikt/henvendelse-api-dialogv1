package no.nav.henvendelse.config

import no.finn.unleash.FakeUnleash
import no.nav.common.health.selftest.SelfTestCheck
import no.nav.henvendelse.consumer.unleash.UnleashService
import no.nav.henvendelse.consumer.unleash.UnleashServiceImpl
import no.nav.henvendelse.utils.unleash.ByEnvironmentStrategy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
class UnleashConfig {
    @Bean
    @Profile("local")
    fun unleashServiceLocal(): UnleashService = UnleashServiceImpl(FakeUnleash())

    @Bean
    @Profile("!local")
    fun unleashServiceNais(): UnleashService = UnleashServiceImpl(
        ByEnvironmentStrategy()
    )

    @Bean
    fun unleashServiceHealthCheck(unleashService: UnleashService) = SelfTestCheck(
        "Unleash",
        false,
        unleashService
    )
}
