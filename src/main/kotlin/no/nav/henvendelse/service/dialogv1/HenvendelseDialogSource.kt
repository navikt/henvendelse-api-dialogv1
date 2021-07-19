package no.nav.henvendelse.service.dialogv1

import no.nav.tjeneste.virksomhet.dialog.v1.DialogV1

class HenvendelseDialogSource(private val dialog: DialogV1) : DialogV1Source {
    override fun hentDialoger(fnr: String, antall: Int): List<Dialog> {
        TODO("Not yet implemented")
    }
}
