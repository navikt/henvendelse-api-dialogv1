package no.nav.henvendelse.config

import no.nav.common.cxf.CXFClient
import no.nav.common.cxf.StsConfig
import no.nav.common.health.selftest.SelfTestCheck
import no.nav.common.utils.EnvironmentUtils
import no.nav.henvendelse.service.dialog.HenvendelseDialogService
import no.nav.henvendelse.utils.CXFClient
import no.nav.henvendelse.utils.createSelfTestCheck
import no.nav.tjeneste.virksomhet.dialog.v1.DialogV1
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.xml.namespace.QName

@Configuration
class HenvendelseConfig {
    @Autowired
    lateinit var stsConfig: StsConfig

    @Bean
    fun henvendelseDialogService() = HenvendelseDialogService(
        createDialogV1Porttype()
            .configureStsForSystemUser(stsConfig)
            .build()
    )

    @Bean
    fun dialogV1PorttypePing(): SelfTestCheck =
        createDialogV1Porttype()
            .configureStsForSystemUser(stsConfig)
            .build()
            .let { porttype ->
                createSelfTestCheck(
                    description = "Henvendelse - DialogV1",
                    critical = true,
                    test = { porttype.ping() }
                )
            }

    companion object {
        fun createDialogV1Porttype(): CXFClient<DialogV1> =
            CXFClient<DialogV1>()
                .wsdl("classpath:wsdl/no/nav/tjeneste/virksomhet/dialog/v1/Binding.wsdl")
                .address(EnvironmentUtils.getRequiredProperty("HENVENDELSE_DIALOG_V1_URL"))
                .serviceName(QName("http://nav.no/tjeneste/virksomhet/dialog/v1/Binding", "Dialog_v1"))
                .endpointName(QName("http://nav.no/tjeneste/virksomhet/dialog/v1/Binding", "Dialog_v1Port"))
    }
}
