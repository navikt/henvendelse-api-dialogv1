package no.nav.henvendelse.service.dialogv1

import no.nav.common.cxf.CXFClient
import no.nav.common.cxf.StsConfig
import no.nav.common.utils.EnvironmentUtils
import no.nav.tjeneste.virksomhet.dialog.v1.DialogV1
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DialogV1Config {
    @Autowired
    lateinit var stsConfig: StsConfig

    @Bean
    fun henvendelseDialogSource() = HenvendelseDialogSource(
        createDialogV1Porttype()
            .configureStsForSystemUser(stsConfig)
            .build()
    )

    @Bean
    fun sfDialogSource() = SfDialogSource()

    private fun createDialogV1Porttype(): CXFClient<DialogV1> =
        CXFClient(DialogV1::class.java)
            .wsdl("classpath:wsdl/no/nav/tjeneste/cirksomhet/dialog/v1/Binding.wsdl")
            .address(EnvironmentUtils.getRequiredProperty("HENVENDELSE_DIALOG_V1_URL"))
}
