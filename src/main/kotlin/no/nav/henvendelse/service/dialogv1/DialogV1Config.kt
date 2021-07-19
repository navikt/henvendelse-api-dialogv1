package no.nav.henvendelse.service.dialogv1

import no.nav.common.cxf.CXFClient
import no.nav.common.cxf.StsConfig
import no.nav.common.utils.EnvironmentUtils
import no.nav.henvendelse.utils.CXFClient
import no.nav.henvendelse.utils.Pingable
import no.nav.henvendelse.utils.createPingable
import no.nav.tjeneste.virksomhet.dialog.v1.DialogV1
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.xml.namespace.QName

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
    fun dialogV1PorttypePing(): Pingable {
        val porttype = createDialogV1Porttype()
            .configureStsForSystemUser(stsConfig)
            .build()

        return createPingable(
            description = "DialogV1PortType",
            critical = true,
            test = { porttype.ping() }
        )
    }

    @Bean
    fun sfDialogSource() = SfDialogSource()

    private fun createDialogV1Porttype(): CXFClient<DialogV1> =
        CXFClient<DialogV1>()
            .wsdl("classpath:wsdl/no/nav/tjeneste/virksomhet/dialog/v1/Binding.wsdl")
            .address(EnvironmentUtils.getRequiredProperty("HENVENDELSE_DIALOG_V1_URL"))
            .serviceName(QName("http://nav.no/tjeneste/virksomhet/dialog/v1/Binding", "Dialog_v1"))
            .endpointName(QName("http://nav.no/tjeneste/virksomhet/dialog/v1/Binding", "Dialog_v1Port"))
}
