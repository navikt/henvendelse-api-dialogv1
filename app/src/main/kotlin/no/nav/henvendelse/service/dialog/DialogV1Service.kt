package no.nav.henvendelse.service.dialog

import no.nav.tjeneste.virksomhet.dialog.v1.informasjon.WSDialog

interface DialogV1Service {
    fun hentDialoger(fnr: String, antall: Int?): List<WSDialog>
}