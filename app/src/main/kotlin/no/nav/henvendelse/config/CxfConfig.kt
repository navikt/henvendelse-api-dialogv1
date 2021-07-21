package no.nav.henvendelse.config

import no.nav.henvendelse.consumer.unleash.UnleashService
import no.nav.henvendelse.service.dialog.HenvendelseDialogService
import no.nav.henvendelse.service.dialog.SfDialogService
import no.nav.henvendelse.soap.DialogWs
import no.nav.henvendelse.soap.infra.SoapServlet
import no.nav.tjeneste.virksomhet.dialog.v1.DialogV1
import org.springframework.boot.web.servlet.ServletRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CxfConfig {
    @Bean
    fun dialogV1(
        henvendelseSource: HenvendelseDialogService,
        sfSource: SfDialogService,
        unleashService: UnleashService
    ): DialogV1 = DialogWs(
        henvendelseSource,
        sfSource,
        unleashService
    )

    @Bean
    fun registerCXF(dialogV1: DialogV1): ServletRegistrationBean<SoapServlet> {
        val servlet = SoapServlet(
            mapOf(
                "/Dialog_v1" to dialogV1
            )
        )
        return ServletRegistrationBean(servlet, "/ws/*")
            .also { it.setLoadOnStartup(1) }
    }
}
