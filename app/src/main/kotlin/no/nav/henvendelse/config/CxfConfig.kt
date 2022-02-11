package no.nav.henvendelse.config

import no.nav.common.cxf.StsConfig
import no.nav.common.health.selftest.SelfTestCheck
import no.nav.common.utils.EnvironmentUtils
import no.nav.henvendelse.service.dialog.DialogV1Service
import no.nav.henvendelse.soap.DialogWs
import no.nav.henvendelse.soap.infra.SoapServlet
import no.nav.henvendelse.utils.CXFClient
import no.nav.henvendelse.utils.createSelfTestCheck
import no.nav.tjeneste.virksomhet.dialog.v1.DialogV1
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.servlet.ServletRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import javax.xml.namespace.QName

@Configuration
class CxfConfig {
    @Autowired
    lateinit var stsConfig: StsConfig

    @Bean
    fun dialogV1(sfSource: DialogV1Service): DialogV1 = DialogWs(sfSource)

    @Bean
    @Profile("!local")
    fun dialogV1ProxyPing(): SelfTestCheck =
        CXFClient<DialogV1>()
            .wsdl("classpath:wsdl/no/nav/tjeneste/virksomhet/dialog/v1/Binding.wsdl")
            .address("https://app$envQualifier.adeo.no/henvendelse-api-dialogv1/ws/Dialog_v1")
            .serviceName(QName("http://nav.no/tjeneste/virksomhet/dialog/v1/Binding", "Dialog_v1"))
            .endpointName(QName("http://nav.no/tjeneste/virksomhet/dialog/v1/Binding", "Dialog_v1Port"))
            .configureStsForSystemUser(stsConfig)
            .build()
            .let { porttype ->
                createSelfTestCheck(
                    description = "Selfverification - DialogV1",
                    critical = false,
                    test = { porttype.ping() }
                )
            }

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

    private val envQualifier: String = when (val env = EnvironmentUtils.getRequiredProperty("APP_ENVIRONMENT_NAME")) {
        "p" -> ""
        else -> "-$env"
    }
}
