package no.nav.henvendelse.rest

import no.nav.henvendelse.service.dialog.DialogV1Service
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
    lateinit var dialogV1Service: DialogV1Service

    @GetMapping
    fun hentFraSalesforce(@RequestParam("fnr") fnr: String): List<WSDialog> {
        AuthUtils.assertNotProd()
        AuthUtils.assertAccess()
        return dialogV1Service.hentDialoger(fnr, 5)
    }
}
