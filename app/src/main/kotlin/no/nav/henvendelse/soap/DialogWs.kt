package no.nav.henvendelse.soap

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
import org.springframework.stereotype.Service

@Service
class DialogWs(
    val henvendelseDialogSource: HenvendelseDialogService,
    val sfDialogSource: SfDialogService,
    val unleash: UnleashService
) : DialogV1 {

    override fun hentDialoger(req: WSHentDialogerRequest): WSHentDialogerResponse {
        val (ident, identType) = AuthUtils.assertAccess()

        val brukerSalesforce = unleash.isEnabled("modia.dialogv1.bruker-salesforce-dialoger")
        log.info("$ident ($identType) henter ${req.antall ?: "N/A"} henvendelser via $brukerSalesforce [true=SF, false=henvendelse]")

        val source: DialogV1Service = if (brukerSalesforce) sfDialogSource else henvendelseDialogSource
        val dialoger: List<WSDialog> = source.hentDialoger(req.personIdent, req.antall)
        return WSHentDialogerResponse()
            .withDialogListe(dialoger)
    }

    override fun ping() {
        val (ident, identType) = AuthUtils.assertAccess()
        log.info("Ping gjort av $ident ($identType)")
    }
}
