package no.nav.henvendelse.service.dialog

import no.nav.common.health.HealthCheckResult
import no.nav.common.health.selftest.SelfTestCheck
import no.nav.tjeneste.virksomhet.dialog.v1.DialogV1
import java.time.OffsetDateTime

data class Dialog(
    val behandlingsKjedeId: String,
    val henvendelsetype: String,
    val sisteDialogDato: OffsetDateTime,
    val temagruppe: String,
    val arkivtema: String,
    val enhet: String
)

interface DialogV1Service {
    fun hentDialoger(fnr: String, antall: Int): List<Dialog>
}

class HenvendelseDialogService(private val dialog: DialogV1) : DialogV1Service {
    override fun hentDialoger(fnr: String, antall: Int): List<Dialog> {
        TODO("Not yet implemented")
    }
}

class SfDialogService : DialogV1Service {
    override fun hentDialoger(fnr: String, antall: Int): List<Dialog> {
        // Må ha PDL integrasjon for mapping fnr -> aktorid
        TODO("Not yet implemented")
    }

    val selftestCheck = SelfTestCheck("SF-Henvendelse", true) {
        // TODO Do something to verify it
        HealthCheckResult.healthy()
    }
}
