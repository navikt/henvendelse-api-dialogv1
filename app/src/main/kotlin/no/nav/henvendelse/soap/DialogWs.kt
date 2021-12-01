package no.nav.henvendelse.soap

import no.nav.common.log.MDCConstants
import no.nav.henvendelse.log
import no.nav.henvendelse.service.dialog.DialogV1Service
import no.nav.henvendelse.service.dialog.HenvendelseDialogService
import no.nav.henvendelse.service.dialog.SfDialogService
import no.nav.henvendelse.service.unleash.UnleashService
import no.nav.henvendelse.utils.AuthUtils
import no.nav.tjeneste.virksomhet.dialog.v1.DialogV1
import no.nav.tjeneste.virksomhet.dialog.v1.informasjon.WSDialog
import no.nav.tjeneste.virksomhet.dialog.v1.meldinger.WSHentDialogerRequest
import no.nav.tjeneste.virksomhet.dialog.v1.meldinger.WSHentDialogerResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Service

@Service
class DialogWs(
    val henvendelseDialogSource: HenvendelseDialogService,
    val sfDialogSource: SfDialogService,
    val unleash: UnleashService
) : DialogV1 {
    private val tjenestekallLogg = LoggerFactory.getLogger("SecureLog")
    override fun hentDialoger(req: WSHentDialogerRequest): WSHentDialogerResponse {
        val (ident, identType) = AuthUtils.assertAccess()

        val brukerSalesforce = unleash.isEnabled("modia.dialogv1.bruker-salesforce-dialoger")
        val debugBrukerSalesforce = unleash.isEnabled("modia.dialogv1.debug-bruker-salesforce-dialoger")
        val kildesystem = if (brukerSalesforce) "SF" else "Henvendelse"
        log.info("$ident ($identType) henter ${req.antall ?: "N/A"} henvendelser via $kildesystem")

        val source: DialogV1Service = if (brukerSalesforce) sfDialogSource else henvendelseDialogSource
        if (!brukerSalesforce && debugBrukerSalesforce) {
            runCatching {
                sfDialogSource.hentDialoger(req.personIdent, req.antall)
            }.fold(
                onSuccess = { tjenestekallLogg.info("[SF-DEBUG] Callid: ${MDC.get(MDCConstants.MDC_CALL_ID)}\nReq: ${req.personIdent} returnerte ${it.size}") },
                onFailure = { tjenestekallLogg.error("[SF-DEBUG] Callid: ${MDC.get(MDCConstants.MDC_CALL_ID)}\nReq: ${req.personIdent} returnerte: ", it) }
            )
        }
        val dialoger: List<WSDialog> = source.hentDialoger(req.personIdent, req.antall)
        return WSHentDialogerResponse()
            .withDialogListe(dialoger)
    }

    override fun ping() {
        val (ident, identType) = AuthUtils.assertAccess()
        log.info("Ping gjort av $ident ($identType)")
    }
}
