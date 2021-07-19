package no.nav.henvendelse.config

import no.nav.henvendelse.service.dialogv1.HenvendelseDialogSource
import no.nav.henvendelse.service.dialogv1.SfDialogSource
import no.nav.henvendelse.soap.DialogWs
import no.nav.henvendelse.soap.infra.SoapServlet
import no.nav.tjeneste.virksomhet.dialog.v1.DialogV1
import org.springframework.boot.web.servlet.ServletRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CxfConfig {
    @Bean
    fun dialogV1(henvendelseSource: HenvendelseDialogSource, sfSource: SfDialogSource): DialogV1 = DialogWs(
        henvendelseSource,
        sfSource
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
