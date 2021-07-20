package no.nav.henvendelse.consumer.pdl

import no.nav.common.rest.client.RestClient
import no.nav.common.sts.SystemUserTokenProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PdlConfig {

    @Bean
    fun pdlService(stsService: SystemUserTokenProvider) = PdlService(
        httpClient = RestClient.baseClientBuilder().build(),
        stsService = stsService
    )

    @Bean
    fun pdlPingable(pdlService: PdlService) = pdlService.selftestCheck
}
