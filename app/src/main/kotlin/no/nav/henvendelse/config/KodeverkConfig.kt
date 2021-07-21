package no.nav.henvendelse.config

import no.nav.common.health.HealthCheckResult
import no.nav.common.health.selftest.SelfTestCheck
import no.nav.common.rest.client.RestClient
import no.nav.common.utils.EnvironmentUtils
import no.nav.henvendelse.consumer.kodeverk.generated.apis.KodeverkApi
import no.nav.henvendelse.service.kodeverk.KodeverkService
import no.nav.henvendelse.service.kodeverk.KodeverkServiceImpl
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class KodeverkConfig {
    @Bean
    fun kodeverkService(): KodeverkService {
        val url = EnvironmentUtils.getRequiredProperty("KODEVERK_URL")
        return KodeverkServiceImpl(
            KodeverkApi(
                basePath = url,
                httpClient = RestClient.baseClient()
            )
        )
    }

    @Bean
    fun kodeverkServicePing(kodeverkService: KodeverkService) = SelfTestCheck(
        "Kodeverk",
        true) {
        if (kodeverkService.error == null) {
            HealthCheckResult.healthy()
        }else {
            HealthCheckResult.unhealthy("Kunne ikke hente kodeverk", kodeverkService.error)
        }
    }
}