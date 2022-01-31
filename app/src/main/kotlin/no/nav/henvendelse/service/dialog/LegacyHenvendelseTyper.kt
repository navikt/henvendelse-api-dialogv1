package no.nav.henvendelse.service.dialog

import no.nav.henvendelse.consumer.sfhenvendelse.generated.models.HenvendelseDTO
import no.nav.henvendelse.consumer.sfhenvendelse.generated.models.MeldingDTO
import no.nav.henvendelse.consumer.sfhenvendelse.generated.models.MeldingFraDTO

enum class LegacyHenvendelseTyper(val behandlingsType: String) {
    SPORSMAL_SKRIFTLIG("ae0060"),
    SVAR_SBL_INNGAAENDE("ae0100"),
    SPORSMAL_MODIA_UTGAAENDE("ae0099"),
    SVAR_SKRIFTLIG("ae0061"),
    REFERAT_TELEFON("ae0063"),
    REFERAT_OPPMOTE("ae0062");

    companion object {
        fun from(henvendelse: HenvendelseDTO, melding: MeldingDTO): LegacyHenvendelseTyper {
            val erForsteMelding = henvendelse.meldinger?.firstOrNull() == melding
            return when (henvendelse.henvendelseType) {
                HenvendelseDTO.HenvendelseType.SAMTALEREFERAT -> {
                    when (melding.kanal) {
                        MeldingDTO.Kanal.OPPMOTE -> REFERAT_OPPMOTE
                        MeldingDTO.Kanal.TELEFON -> REFERAT_TELEFON
                        MeldingDTO.Kanal.DIGITAL -> REFERAT_TELEFON
                        else -> REFERAT_TELEFON
                    }
                }
                HenvendelseDTO.HenvendelseType.MELDINGSKJEDE -> {
                    when (melding.fra.identType) {
                        MeldingFraDTO.IdentType.AKTORID -> if (erForsteMelding) SPORSMAL_SKRIFTLIG else SVAR_SBL_INNGAAENDE
                        MeldingFraDTO.IdentType.NAVIDENT -> if (erForsteMelding) SPORSMAL_MODIA_UTGAAENDE else SVAR_SKRIFTLIG
                        MeldingFraDTO.IdentType.SYSTEM -> SVAR_SKRIFTLIG
                    }
                }
            }
        }
    }
}
