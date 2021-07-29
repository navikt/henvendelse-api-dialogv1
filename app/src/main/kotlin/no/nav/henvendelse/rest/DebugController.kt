package no.nav.henvendelse.rest

import no.nav.henvendelse.service.dialog.HenvendelseDialogService
import no.nav.henvendelse.service.dialog.SfDialogService
import no.nav.henvendelse.utils.AuthUtils
import no.nav.tjeneste.virksomhet.dialog.v1.informasjon.WSDialog
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal/debug")
class DebugController {
    @Autowired
    lateinit var henvendelse: HenvendelseDialogService

    @Autowired
    lateinit var sfHenvendelse: SfDialogService

    @GetMapping("/henvendelse")
    fun hentFraHenvendelse(@RequestParam("fnr") fnr: String): List<WSDialog> {
        AuthUtils.assertNotProd()
        return henvendelse.hentDialoger(fnr, 5)
    }

    @GetMapping("/salesforce")
    fun hentFraSalesforce(@RequestParam("fnr") fnr: String): List<WSDialog> {
        AuthUtils.assertNotProd()
        return sfHenvendelse.hentDialoger(fnr, 5)
    }
}
