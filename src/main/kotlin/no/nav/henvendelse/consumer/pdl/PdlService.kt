package no.nav.henvendelse.consumer.pdl

import no.nav.common.health.HealthCheckResult
import no.nav.common.health.selftest.SelfTestCheck
import no.nav.common.sts.SystemUserTokenProvider
import no.nav.common.utils.EnvironmentUtils
import no.nav.henvendelse.APPLICATION_NAME
import no.nav.henvendelse.consumer.pdl.queries.HentAktorId
import no.nav.henvendelse.utils.FileUtils
import okhttp3.OkHttpClient
import okhttp3.Request

class PdlException(message: String, cause: Throwable) : RuntimeException(message, cause)

class PdlService(val httpClient: OkHttpClient, val stsService: SystemUserTokenProvider) {
    private val pdlUrl: String = EnvironmentUtils.getRequiredProperty("PDL_URL")
    private val graphQLClient = GraphQLClient(
        httpClient = httpClient,
        config = GraphQLClientConfig(
            tjenesteNavn = "PDL",
            requestConfig = {
                callId ->

                url(pdlUrl)
                addHeader("Nav-Call-Id", callId)
                addHeader("Nav-Consumer-Id", APPLICATION_NAME)
                addHeader("Nav-Consumer-Token", "Bearer ${stsService.systemUserToken}")
                addHeader("Authorization", "Bearer ${stsService.systemUserToken}")
                addHeader("Tema", "GEN")
            }
        )
    )

    fun hentAktorId(fnr: String): List<String> {
        return graphQLClient
            .runCatching {
                execute(
                    HentAktorId(
                        HentAktorId.Variables(fnr)
                    )
                )
            }
            .mapCatching { response ->
                response
                    .data
                    ?.hentIdenter
                    ?.identer
                    ?.map { it.ident }
                    ?: throw IllegalStateException("Fant ingen identer for $fnr")
            }
            .getOrThrow { PdlException("Kunne ikke hente aktorId for $fnr", it) }
    }

    val selftestCheck = SelfTestCheck("PDL", true) {
        try {
            pingGraphQL()
            HealthCheckResult.healthy()
        } catch (e: Throwable) {
            HealthCheckResult.unhealthy(e)
        }
    }

    private fun pingGraphQL(): Int {
        val request = Request.Builder()
            .url(pdlUrl)
            .method("OPTIONS", null)
            .build()
        val response = httpClient.newCall(request).execute()
        return response
            .also { runCatching { it.close() } }
            .code()
    }

    companion object {
        fun lastQueryFraFil(name: String): String {
            return FileUtils.readFileContent("/pdl/$name.graphql")
                .replace("[\n\r]", "")
        }
    }
}

private inline fun <T> Result<T>.getOrThrow(fn: (Throwable) -> Throwable): T {
    val exception = exceptionOrNull()
    if (exception != null) {
        throw fn(exception)
    }
    return getOrThrow()
}
