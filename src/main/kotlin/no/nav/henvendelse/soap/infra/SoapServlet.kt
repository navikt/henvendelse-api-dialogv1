package no.nav.henvendelse.soap.infra

import no.nav.common.cxf.CXFEndpoint
import no.nav.common.cxf.saml.CXFServletWithAuth
import org.apache.cxf.BusFactory
import org.apache.cxf.logging.FaultListener
import javax.servlet.ServletConfig

class SoapServlet(val endpoints: Map<String, Any>) : CXFServletWithAuth() {
    override fun loadBus(sc: ServletConfig?) {
        super.loadBus(sc)
        BusFactory.setDefaultBus(getBus())
        endpoints.forEach(::loadWSService)
    }

    private fun loadWSService(url: String, serviceBean: Any) {
        val endpoint = CXFEndpoint()
            .address(url)
            .serviceBean(serviceBean)

        endpoint.factoryBean.invoker = MethodInvokerMedFeilhandtering(serviceBean)
        endpoint.setProperty(FaultListener::class.java.name, SoapFaultListener())

        endpoint.create()
    }
}
