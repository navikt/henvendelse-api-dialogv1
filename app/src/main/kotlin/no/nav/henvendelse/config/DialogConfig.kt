package no.nav.henvendelse.config

import no.nav.common.health.selftest.SelfTestCheck
import no.nav.common.rest.client.RestClient
import no.nav.common.sts.SystemUserTokenProvider
import no.nav.common.utils.EnvironmentUtils
import no.nav.henvendelse.consumer.sfhenvendelse.generated.apis.HenvendelseInfoApi
import no.nav.henvendelse.consumer.sfhenvendelse.generated.apis.KodeverkApi
import no.nav.henvendelse.log
import no.nav.henvendelse.service.dialog.DialogV1ServiceImpl
import no.nav.henvendelse.service.kodeverk.KodeverkService
import no.nav.henvendelse.service.pdl.PdlService
import no.nav.henvendelse.utils.AuthUtils
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.lang.StringBuilder

@Configuration
class DialogConfig {
    @Bean
    fun sfDialogService(
        stsService: SystemUserTokenProvider,
        pdlService: PdlService,
        kodeverkService: KodeverkService
    ): DialogV1ServiceImpl {
        val url = EnvironmentUtils.getRequiredProperty("SF_HENVENDELSE_URL")
        val urlAlt = EnvironmentUtils.getRequiredProperty("SF_HENVENDELSE_URL_ALT")
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
                AuthUtils.ifInternUser { ident ->
                    builder.addHeader("Nav-Ident", ident)
                }
                chain.proceed(builder.build())
            }
            .build()

        return DialogV1ServiceImpl(
            HenvendelseInfoApi(url, httpClient),
            HenvendelseInfoApi(urlAlt, httpClient),
            KodeverkApi(url, httpClient),
            pdlService,
            kodeverkService
        )
    }

    @Bean
    fun dialogServiceSelfTestCheck(dialogV1ServiceImpl: DialogV1ServiceImpl): SelfTestCheck = dialogV1ServiceImpl.selftestCheck
}
