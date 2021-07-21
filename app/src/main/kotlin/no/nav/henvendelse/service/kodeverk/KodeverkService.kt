package no.nav.henvendelse.service.kodeverk

import no.nav.henvendelse.APPLICATION_NAME
import no.nav.henvendelse.consumer.kodeverk.generated.apis.KodeverkApi
import no.nav.henvendelse.consumer.kodeverk.generated.models.BeskrivelseDTO
import no.nav.henvendelse.consumer.kodeverk.generated.models.BetydningDTO
import no.nav.henvendelse.consumer.kodeverk.generated.models.GetKodeverkKoderBetydningerResponseDTO
import no.nav.henvendelse.log
import java.util.*

interface KodeverkService {
    fun hentVerdi(kodeverk: Kodeverk, termKode: String?, spraak: String = "nb"): String
    var error: Throwable?
}

enum class Kodeverk(val kodeverknavn: String) {
    TEMAGRUPPER("TemagrupperMidlertidig"),
    BEHANDLINGSTYPER("Behandlingstyper"),
    ARKIVTEMAER("Arkivtemaer")
}

class KodeverkServiceImpl(private val api: KodeverkApi) : KodeverkService {
    override var error: Throwable? = null
    val kodeverkMap: Map<Kodeverk, GetKodeverkKoderBetydningerResponseDTO?> = Kodeverk.values()
        .associate { it to hentKodeverk(it.kodeverknavn) }

    override fun hentVerdi(kodeverk: Kodeverk, termKode: String?, spraak: String): String {
        if (termKode == null) {
            return ""
        }

        val response = kodeverkMap[kodeverk]
        val betydninger: BetydningDTO? = response?.betydninger?.get(termKode)?.firstOrNull()
        val beskrivelse: BeskrivelseDTO? = betydninger?.beskrivelser?.get(spraak)
        return beskrivelse?.term ?: ""
    }

    fun hentKodeverk(kodeverk: String): GetKodeverkKoderBetydningerResponseDTO? {
        return api.runCatching {
            betydningUsingGET(
                navCallId = UUID.randomUUID().toString(),
                navConsumerId = APPLICATION_NAME,
                kodeverksnavn = kodeverk,
                spraak = listOf("nb")
            )
        }
            .onFailure {
                this@KodeverkServiceImpl.error = it
                log.error("Feil ved henting av kodeverk '$kodeverk'", it)
            }
            .getOrNull()
    }
}
