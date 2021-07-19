package no.nav.henvendelse.service.dialogv1

import java.time.OffsetDateTime

data class Dialog(
    val behandlingsKjedeId: String,
    val henvendelsetype: String,
    val sisteDialogDato: OffsetDateTime,
    val temagruppe: String,
    val arkivtema: String,
    val enhet: String
)

interface DialogV1Source {
    fun hentDialoger(fnr: String, antall: Int): List<Dialog>
}
