package no.nav.henvendelse

import com.expediagroup.graphql.types.GraphQLResponse
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.PlainJWT
import no.nav.common.json.JsonUtils
import no.nav.common.nais.NaisYamlUtils.loadFromYaml
import no.nav.common.test.ssl.SSLTestUtils
import no.nav.common.utils.SslUtils
import no.nav.henvendelse.consumer.pdl.generated.HentAktorId
import org.springframework.boot.SpringApplication
import java.util.*

fun main(args: Array<String>) {
    loadVaultSecrets()
    loadFromYaml(".nais/preprod.yaml")
    SslUtils.setupTruststore()
    SSLTestUtils.disableCertificateChecks()

    startExternalMocks()

    val application = SpringApplication(Application::class.java)
    application.setAdditionalProfiles("local")
    application.run(*args)
}

private fun loadVaultSecrets() {
    System.setProperty(SERVICEUSER_USERNAME_PROPERTY, "dummy")
    System.setProperty(SERVICEUSER_PASSWORD_PROPERTY, "dummy")
}

private fun startExternalMocks(): WireMockServer {
    val server = WireMockServer()
    server.start()
    server.also(::addStsMock)
    server.also(::addPdlMock)
    return server
}

private fun addStsMock(server: WireMockServer) {
    val oidcConfig = JsonUtils.toJson(
        mapOf(
            "token_endpoint" to "http://localhost:${server.port()}/token"
        )
    )
    val accessToken = JsonUtils.toJson(
        mapOf(
            "access_token" to PlainJWT(
                JWTClaimsSet.Builder()
                    .issuer("issuer")
                    .audience("audience")
                    .subject("system-user")
                    .issueTime(Date())
                    .expirationTime(Date(System.currentTimeMillis() + (10 * 60 * 1000)))
                    .build()
            ).serialize()
        )
    )

    server.stubFor(
        get(urlPathEqualTo("/.well-known/openid-configuration"))
            .willReturn(aResponse().withStatus(200).withBody(oidcConfig))
    )
    server.stubFor(
        get(urlPathEqualTo("/token"))
            .willReturn(aResponse().withStatus(200).withBody(accessToken))
    )
    System.setProperty("SECURITY_TOKEN_SERVICE_DISCOVERY_URL", "http://localhost:${server.port()}/.well-known/openid-configuration")
}

private fun addPdlMock(server: WireMockServer) {
    server.stubFor(
        options(urlPathEqualTo("/pdl/graphql"))
            .willReturn(aResponse().withStatus(200))
    )

    val body = JsonUtils.toJson(
        GraphQLResponse(
            errors = null,
            data = HentAktorId.Result(
                HentAktorId.Identliste(
                    listOf(HentAktorId.IdentInformasjon("00012345679000"))
                )
            )
        )
    )
    server.stubFor(
        get(urlPathEqualTo("/pdl/graphql"))
            .willReturn(aResponse().withStatus(200).withBody(body))
    )
    System.setProperty("PDL_URL", "http://localhost:${server.port()}/pdl/graphql")
}
