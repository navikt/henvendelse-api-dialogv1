package no.nav.henvendelse.soap.infra

import net.logstash.logback.encoder.org.apache.commons.lang.exception.ExceptionUtils
import no.nav.common.json.JsonUtils
import no.nav.common.types.feil.Feil
import no.nav.common.types.feil.FeilDTO
import no.nav.common.types.feil.FeilType
import no.nav.common.utils.EnvironmentUtils
import no.nav.henvendelse.log
import org.apache.cxf.jaxws.JAXWSMethodInvoker
import java.io.StringWriter
import java.util.*
import javax.ws.rs.NotAuthorizedException
import javax.ws.rs.NotFoundException
import javax.xml.soap.SOAPException
import javax.xml.soap.SOAPFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.ws.soap.SOAPFaultException

class MethodInvokerMedFeilhandtering(serviceBean: Any) : JAXWSMethodInvoker(serviceBean) {
    private val soapFactory: SOAPFactory = SOAPFactory.newInstance()

    override fun findSoapFaultException(throwable: Throwable): SOAPFaultException {
        return try {
            val fault = soapFactory.createFault()
            fault.faultString = UUID.randomUUID().toString()
            fault.faultCode = getType(throwable).name
            if (EnvironmentUtils.isDevelopment().orElse(false)) {
                val detaljer = JsonUtils.toJson(finnDetaljer(throwable))
                if (detaljer.isNullOrEmpty()) {
                    fault.detail.addTextNode(detaljer)
                }
            }
            return SOAPFaultException(fault)
        } catch (e: SOAPException) {
            log.error(e.message, e)
            super.findSoapFaultException(throwable)
        }
    }

    private fun getType(throwable: Throwable): Feil.Type =
        when (throwable) {
            is Feil -> throwable.type
            is SOAPFaultException -> valueOf<FeilType>(throwable.fault?.faultCodeAsName?.localName) ?: FeilType.UKJENT
            is IllegalArgumentException -> FeilType.UGYLDIG_REQUEST
            is NotFoundException -> FeilType.FINNES_IKKE
            is NotAuthorizedException -> FeilType.INGEN_TILGANG
            else -> FeilType.UKJENT
        }

    private fun finnDetaljer(throwable: Throwable) = FeilDTO.Detaljer(
        throwable::class.java.name,
        throwable.message,
        finnStacktrace(throwable)
    )

    private fun finnStacktrace(throwable: Throwable): String {
        return when (throwable) {
            is SOAPFaultException -> finnSoapStacktrace(throwable)
            else -> ExceptionUtils.getStackTrace(throwable)
        }
    }

    private fun finnSoapStacktrace(throwable: SOAPFaultException): String {
        return try {
            val writer = StringWriter()
            val transformer = TransformerFactory.newInstance().newTransformer()
            transformer.setOutputProperty(OutputKeys.INDENT, "yes")
            transformer.transform(DOMSource(throwable.fault), StreamResult(writer))
            writer.toString()
        } catch (e: Exception) {
            log.error("Kunne ikke hente ut stacktrace fra SOAPFaultException", e)
            throwable.message ?: "Ukjent SOAP feil, og kunne ikke hente ut stacktrace."
        }
    }

    private inline fun <reified T : Enum<*>> valueOf(name: String?): T? {
        return T::class.java.enumConstants
            .find { it.name == name }
    }
}
