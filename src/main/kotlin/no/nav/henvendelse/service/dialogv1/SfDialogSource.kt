package no.nav.henvendelse.service.dialogv1

class SfDialogSource : DialogV1Source {
    override fun hentDialoger(fnr: String, antall: Int): List<Dialog> {
        // Må ha PDL integrasjon for mapping fnr -> aktorid
        TODO("Not yet implemented")
    }
}
