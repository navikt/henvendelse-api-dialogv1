package no.nav.henvendelse.consumer.pdl.queries

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue
import no.nav.henvendelse.consumer.pdl.GraphQLRequest
import no.nav.henvendelse.consumer.pdl.GraphQLResult
import no.nav.henvendelse.consumer.pdl.GraphQLVariables
import no.nav.henvendelse.consumer.pdl.PdlService.Companion.lastQueryFraFil

class HentAktorId(override val variables: Variables) :
    GraphQLRequest<HentAktorId.Variables, HentAktorId.Result> {
    override val query: String = lastQueryFraFil("hentAktorId")
    override val expectedReturnType: Class<Result> = Result::class.java

    data class Variables(val fnr: String) : GraphQLVariables

    enum class IdentGruppe {
        AKTORID,
        FOLKEREGISTERIDENT,
        NPID,
        @JsonEnumDefaultValue __UNKNOWN_VALUE;
    }
    data class IdentInformasjon(val ident: String)
    data class Identliste(val identer: List<IdentInformasjon>)
    data class Result(val hentIdenter: Identliste) : GraphQLResult
}
