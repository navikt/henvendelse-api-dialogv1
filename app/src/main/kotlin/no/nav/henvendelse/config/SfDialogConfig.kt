package no.nav.henvendelse.config

import no.nav.common.health.selftest.SelfTestCheck
import no.nav.common.rest.client.RestClient
import no.nav.common.sts.SystemUserTokenProvider
import no.nav.common.utils.EnvironmentUtils
import no.nav.henvendelse.consumer.sfhenvendelse.generated.apis.HenvendelseInfoApi
import no.nav.henvendelse.consumer.sfhenvendelse.generated.apis.KodeverkApi
import no.nav.henvendelse.log
import no.nav.henvendelse.service.dialog.SfDialogService
import no.nav.henvendelse.service.kodeverk.KodeverkService
import no.nav.henvendelse.service.pdl.PdlService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.lang.StringBuilder

@Configuration
class SfDialogConfig {
    @Bean
    fun sfDialogService(
        stsService: SystemUserTokenProvider,
        pdlService: PdlService,
        kodeverkService: KodeverkService
    ): SfDialogService {
        val url = EnvironmentUtils.getRequiredProperty("SF_HENVENDELSE_URL")
        val httpClient = RestClient.baseClient().newBuilder()
            .addInterceptor { chain ->
                val request = chain.request()
                val output = StringBuilder()
                output.append("[SF-Henvendelse] Url: ${request.url()}\n")
                val response = runCatching { chain.proceed(request) }
                    .onFailure { exception -> output.append("Exception: ${exception.message}\n") }
                    .onSuccess { response -> output.append("Response-Size: ${response.peekBody(Long.MAX_VALUE).contentLength()}") }
                    .getOrThrow()
                output.append("Status: ${response.code()}")
                log.info(output.toString())
                response
            }
            .addInterceptor { chain ->
                val builder = chain.request().newBuilder()
                builder.addHeader("Authorization", "Bearer ${stsService.systemUserToken}")
                chain.proceed(builder.build())
            }
            .build()

        return SfDialogService(
            HenvendelseInfoApi(url, httpClient),
            KodeverkApi(url, httpClient),
            pdlService,
            kodeverkService
        )
    }

    @Bean
    fun sfDialogServiceSelfTestCheck(sfDialogService: SfDialogService): SelfTestCheck = sfDialogService.selftestCheck
}
