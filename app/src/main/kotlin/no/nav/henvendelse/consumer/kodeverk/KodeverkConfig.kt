package no.nav.henvendelse.consumer.kodeverk

import no.nav.common.rest.client.RestClient
import no.nav.common.utils.EnvironmentUtils
import no.nav.henvendelse.APPLICATION_NAME
import no.nav.henvendelse.consumer.kodeverk.generated.apis.KodeverkApi
import no.nav.henvendelse.consumer.kodeverk.generated.models.BeskrivelseDTO
import no.nav.henvendelse.consumer.kodeverk.generated.models.BetydningDTO
import no.nav.henvendelse.consumer.kodeverk.generated.models.GetKodeverkKoderBetydningerResponseDTO
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.*

@Configuration
class KodeverkConfig {
    @Bean
    fun kodeverkService(): KodeverkService {
        val url = EnvironmentUtils.getRequiredProperty("KODEVERK_URL")
        return KodeverkServiceImpl(
            KodeverkApi(
                basePath = url,
                httpClient = RestClient.baseClient()
            )
        )
    }
}

interface KodeverkService {
    fun getBehandlingstypeNavn(behandlingsType: String?): String
    fun getTemagruppeNavn(temagruppe: String?): String
    fun getArkivtemaNavn(temakode: String?): String
}
enum class Kodeverk(val kodeverknavn: String) {
    TEMAGRUPPER("TemagrupperMidlertidig"),
    BEHANDLINGSTYPER("Behandlingstyper"),
    ARKIVTEMAER("Arkivtemaer")
}
class KodeverkServiceImpl(private val api: KodeverkApi) : KodeverkService {
    val kodeverkMap: Map<Kodeverk, GetKodeverkKoderBetydningerResponseDTO> = Kodeverk.values()
        .associate { it to hentKodeverk(it.kodeverknavn) }

    override fun getBehandlingstypeNavn(behandlingsType: String?): String {
        return hentVerdi(Kodeverk.BEHANDLINGSTYPER, behandlingsType)
    }

    override fun getTemagruppeNavn(temagruppe: String?): String {
        return hentVerdi(Kodeverk.TEMAGRUPPER, temagruppe)
    }

    override fun getArkivtemaNavn(temakode: String?): String {
        return hentVerdi(Kodeverk.ARKIVTEMAER, temakode)
    }

    fun hentVerdi(kodeverk: Kodeverk, termKode: String?, spraak: String = "nb"): String {
        if (termKode == null) {
            return ""
        }

        val response = requireNotNull(kodeverkMap[kodeverk])
        val betydninger: BetydningDTO? = response.betydninger[termKode]?.firstOrNull()
        val beskrivelse: BeskrivelseDTO? = betydninger?.beskrivelser?.get(spraak)
        return beskrivelse?.term ?: ""
    }

    fun hentKodeverk(kodeverk: String): GetKodeverkKoderBetydningerResponseDTO {
        return api.betydningUsingGET(
            navCallId = UUID.randomUUID().toString(),
            navConsumerId = APPLICATION_NAME,
            kodeverksnavn = kodeverk,
            spraak = listOf("nb")
        )
    }
}
