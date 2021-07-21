package no.nav.henvendelse.soap.infra

import no.nav.henvendelse.log
import org.apache.cxf.logging.FaultListener
import org.apache.cxf.message.Message
import java.lang.Exception

class SoapFaultListener : FaultListener {
    override fun faultOccurred(exception: Exception?, description: String?, message: Message?): Boolean {
        log.error(description, exception)
        return true
    }
}
