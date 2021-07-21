package no.nav.henvendelse.service.dialog

import no.nav.common.health.HealthCheckResult
import no.nav.common.health.selftest.SelfTestCheck
import no.nav.common.log.MDCConstants
import no.nav.henvendelse.consumer.sfhenvendelse.generated.apis.HenvendelseInfoApi
import no.nav.henvendelse.consumer.sfhenvendelse.generated.apis.KodeverkApi
import no.nav.henvendelse.consumer.sfhenvendelse.generated.models.HenvendelseDTO
import no.nav.henvendelse.consumer.sfhenvendelse.generated.models.MeldingDTO
import no.nav.henvendelse.consumer.sfhenvendelse.generated.models.MeldingFraDTO
import no.nav.henvendelse.service.kodeverk.Kodeverk
import no.nav.henvendelse.service.kodeverk.KodeverkService
import no.nav.henvendelse.service.pdl.PdlService
import no.nav.tjeneste.virksomhet.dialog.v1.informasjon.WSArkivtemaer
import no.nav.tjeneste.virksomhet.dialog.v1.informasjon.WSDialog
import no.nav.tjeneste.virksomhet.dialog.v1.informasjon.WSHenvendelsestyper
import no.nav.tjeneste.virksomhet.dialog.v1.informasjon.WSTemagrupper
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.slf4j.MDC
import java.time.OffsetDateTime
import java.util.*

class SfDialogService(
    private val sfHenvendelse: HenvendelseInfoApi,
    private val sfKodeverk: KodeverkApi,
    private val pdlService: PdlService,
    private val kodeverkService: KodeverkService
) : DialogV1Service {
    val SOSIALE_TEMAGRUPPER = listOf("OKSOS", "ANSOS")
    override fun hentDialoger(fnr: String, antall: Int?): List<WSDialog> {
        val aktorId: String = pdlService.hentAktorIder(fnr).firstNotNullOf { it }

        return sfHenvendelse.henvendelseinfoHenvendelselisteGet(aktorId, callId())
            .asSequence()
            .sortedByDescending {
                (it.meldinger ?: emptyList())
                    .maxByOrNull { melding -> requireNotNull(melding.sendtDato) }
                    ?.sendtDato
            }
            .filter(::erIkkeKassert)
            .filter(::erIkkeFeilsendt)
            .filter(::erIkkeKontorsperret)
            .filter(::erIkkeTilknyttetSosialTjenester)
            .map { tilDialog(fnr, it) }
            .take(antall ?: Int.MAX_VALUE)
            .toList()
    }

    private fun tilDialog(fnr: String, henvendelseDTO: HenvendelseDTO): WSDialog {
        val sisteMeldingDTO: MeldingDTO = requireNotNull(
            henvendelseDTO.meldinger?.maxByOrNull { requireNotNull(it.sendtDato) }
        )
        val fraBruker = sisteMeldingDTO.fra?.identType == MeldingFraDTO.IdentType.AKTORID

        val legacyHenvendelseTyper = LegacyHenvendelseTyper.from(henvendelseDTO, sisteMeldingDTO)
        val henvendelsetype = WSHenvendelsestyper()
            .withValue(legacyHenvendelseTyper.behandlingsType)
            .withTermnavn(kodeverkService.hentVerdi(Kodeverk.BEHANDLINGSTYPER, legacyHenvendelseTyper.behandlingsType))
        val temagruppe: WSTemagrupper = requireNotNull(henvendelseDTO.gjeldendeTemagruppe)
            .let {
                WSTemagrupper()
                    .withValue(it)
                    .withTermnavn(kodeverkService.hentVerdi(Kodeverk.TEMAGRUPPER, it))
            }

        val arkivtema: WSArkivtemaer = requireNotNull(henvendelseDTO.gjeldendeTema)
            .let {
                WSArkivtemaer()
                    .withValue(it)
                    .withTermnavn(kodeverkService.hentVerdi(Kodeverk.ARKIVTEMAER, it))
            }

        return WSDialog()
            .withBehandlingsKjedeId(requireNotNull(henvendelseDTO.kjedeId))
            .withHenvendelsestype(henvendelsetype)
            .withSisteDialogDato(requireNotNull(sisteMeldingDTO.sendtDato).toJodaDateTime())
            .withTemagruppe(temagruppe)
            .withArkivtema(arkivtema)
            .withEnhet(if (fraBruker) fnr else TODO("Mangler felt fra SF"))
    }

    private fun OffsetDateTime.toJodaDateTime(): DateTime {
        return DateTime(
            this.year,
            this.monthValue,
            this.dayOfMonth,
            this.hour,
            this.minute,
            this.second,
            this.nano / 1_000_000,
            DateTimeZone.getDefault()
        )
    }

    private fun erIkkeKassert(henvendelseDTO: HenvendelseDTO): Boolean {
        // TODO Manglende kasserings dato, skal vi anta at det ikke er kassert? Blir forhåpentligvis fikset av strengere API
        return OffsetDateTime.now().isBefore(henvendelseDTO.kasseringsDato ?: OffsetDateTime.MAX)
    }

    private fun erIkkeFeilsendt(henvendelseDTO: HenvendelseDTO): Boolean {
        return !(henvendelseDTO.feilsendt ?: false)
    }

    private fun erIkkeKontorsperret(henvendelseDTO: HenvendelseDTO): Boolean {
        return !(henvendelseDTO.kontorsperre ?: false)
    }

    private fun erIkkeTilknyttetSosialTjenester(henvendelseDTO: HenvendelseDTO): Boolean {
        return !SOSIALE_TEMAGRUPPER.contains(henvendelseDTO.gjeldendeTemagruppe)
    }

    val selftestCheck = SelfTestCheck("SF-Henvendelse", true) {
        try {
            sfKodeverk.henvendelseKodeverkTemagrupperGet(callId())
            HealthCheckResult.healthy()
        } catch (e: Exception) {
            HealthCheckResult.unhealthy("Kunne ikke hente ut kodeverk fra sf-henvendelse", e)
        }
    }

    private fun callId(): String = MDC.get(MDCConstants.MDC_CALL_ID) ?: UUID.randomUUID().toString()
}