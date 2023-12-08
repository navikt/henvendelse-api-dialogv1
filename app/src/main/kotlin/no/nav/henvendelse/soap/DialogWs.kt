package no.nav.henvendelse.soap

import no.nav.henvendelse.log
import no.nav.henvendelse.service.dialog.DialogV1Service
import no.nav.henvendelse.utils.AuthUtils
import no.nav.tjeneste.virksomhet.dialog.v1.DialogV1
import no.nav.tjeneste.virksomhet.dialog.v1.informasjon.WSDialog
import no.nav.tjeneste.virksomhet.dialog.v1.meldinger.WSHentDialogerRequest
import no.nav.tjeneste.virksomhet.dialog.v1.meldinger.WSHentDialogerResponse
import org.springframework.stereotype.Service

@Service
class DialogWs(val dialogService: DialogV1Service) : DialogV1 {
    override fun hentDialoger(req: WSHentDialogerRequest): WSHentDialogerResponse {
        val (ident, identType) = AuthUtils.assertAccess()
        val consumerId = AuthUtils.getConsumerId()
        log.info("Uthenting gjort av $ident ($identType), consumerId $consumerId")

        try {
            val dialoger: List<WSDialog> = dialogService.hentDialoger(req.personIdent, req.antall, consumerId)
            return WSHentDialogerResponse()
                .withDialogListe(dialoger)
        } catch (e: Exception) {
            log.error("Failed fetch dialogService henteDialog, error message: ${e.message}")
            throw IllegalStateException("Failed fetch dialogService henteDialog, error message: ${e.message}")
        }
    }

    override fun ping() {
        val (ident, identType) = AuthUtils.assertAccess()
        log.info("Ping gjort av $ident ($identType)")
    }
}
