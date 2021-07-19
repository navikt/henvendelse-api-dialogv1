package no.nav.henvendelse.soap

import no.nav.henvendelse.service.dialogv1.Dialog
import no.nav.henvendelse.service.dialogv1.DialogV1Source
import no.nav.henvendelse.service.dialogv1.HenvendelseDialogSource
import no.nav.henvendelse.service.dialogv1.SfDialogSource
import no.nav.tjeneste.virksomhet.dialog.v1.DialogV1
import no.nav.tjeneste.virksomhet.dialog.v1.informasjon.WSDialog
import no.nav.tjeneste.virksomhet.dialog.v1.meldinger.WSHentDialogerRequest
import no.nav.tjeneste.virksomhet.dialog.v1.meldinger.WSHentDialogerResponse
import org.springframework.stereotype.Service

const val useHenvendelse = true // TODO erstatte med unleash?

@Service
class DialogWs(
    val henvendelseDialogSource: HenvendelseDialogSource,
    val sfDialogSource: SfDialogSource
) : DialogV1 {

    override fun hentDialoger(req: WSHentDialogerRequest): WSHentDialogerResponse {
        val source: DialogV1Source = if (useHenvendelse) henvendelseDialogSource else sfDialogSource
        val dialoger: List<Dialog> = source.hentDialoger(req.personIdent, req.antall)
        return WSHentDialogerResponse()
            .withDialogListe(dialoger.map(::toWSDialog))
    }

    private fun toWSDialog(dialog: Dialog): WSDialog {
        TODO("Not yet implemented")
    }

    override fun ping() {}
}
