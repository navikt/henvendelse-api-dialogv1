package no.nav.henvendelse.config

import io.ktor.util.*
import no.nav.common.rest.client.RestClient
import no.nav.common.sts.SystemUserTokenProvider
import no.nav.henvendelse.service.pdl.PdlService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@KtorExperimentalAPI
class PdlConfig {
    @Bean
    fun pdlService(stsService: SystemUserTokenProvider) = PdlService(
        httpClient = RestClient.baseClientBuilder().build(),
        stsService = stsService
    )

    @Bean
    fun pdlPingable(pdlService: PdlService) = pdlService.selftestCheck
}
