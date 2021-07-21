package no.nav.henvendelse.service.dialog

import no.nav.tjeneste.virksomhet.dialog.v1.DialogV1
import no.nav.tjeneste.virksomhet.dialog.v1.informasjon.WSDialog
import no.nav.tjeneste.virksomhet.dialog.v1.meldinger.WSHentDialogerRequest

class HenvendelseDialogService(private val dialog: DialogV1) : DialogV1Service {
    override fun hentDialoger(fnr: String, antall: Int?): List<WSDialog> {
        val wsRequest = WSHentDialogerRequest()
            .withPersonIdent(fnr)
            .withAntall(antall)

        return dialog.hentDialoger(wsRequest).dialogListe
    }
}