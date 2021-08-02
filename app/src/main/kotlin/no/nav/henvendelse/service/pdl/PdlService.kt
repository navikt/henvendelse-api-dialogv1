package no.nav.henvendelse.service.pdl

import com.expediagroup.graphql.client.GraphQLClient
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import no.nav.common.health.HealthCheckResult
import no.nav.common.health.selftest.SelfTestCheck
import no.nav.common.log.MDCConstants
import no.nav.common.sts.SystemUserTokenProvider
import no.nav.common.utils.EnvironmentUtils
import no.nav.henvendelse.consumer.pdl.generated.HentAktorId
import no.nav.henvendelse.utils.JacksonUtils
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.MDC
import org.springframework.cache.annotation.Cacheable
import java.net.URL
import java.util.*

class PdlException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

// TODO finn ut hvorfor kotlin-maven-allopen ikke fikser dette
open class PdlService(private val httpClient: OkHttpClient, private val stsService: SystemUserTokenProvider) {
    private val pdlUrl: String = EnvironmentUtils.getRequiredProperty("PDL_URL")
    private val graphQLClient = GraphQLClient(URL(pdlUrl), CIO, JacksonUtils.objectMapper) {}

    @Cacheable("pdl-aktorider")
    open fun hentAktorIder(fnr: String): List<String> = runBlocking {
        val response = HentAktorId(graphQLClient).execute(HentAktorId.Variables(fnr), systemTokenHeaders)
        if (response.errors != null) {
            throw PdlException(response.errors.toString())
        }
        response
            .data
            ?.hentIdenter
            ?.identer
            ?.map { it.ident }
            ?: throw PdlException("AktørId for $fnr ble ikke funnet")
    }

    private val systemTokenHeaders: HttpRequestBuilder.() -> Unit = {
        val systemuserToken: String = stsService.systemUserToken

        header("Nav-Consumer-Token", "Bearer $systemuserToken")
        header("Authorization", "Bearer $systemuserToken")
        header("Tema", "GEN")
        header("Nav-Call-Id", MDC.get(MDCConstants.MDC_CALL_ID) ?: UUID.randomUUID().toString())
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
}
