package no.nav.henvendelse

import com.expediagroup.graphql.types.GraphQLResponse
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.PlainJWT
import no.nav.common.nais.NaisYamlUtils.loadFromYaml
import no.nav.common.test.ssl.SSLTestUtils
import no.nav.common.utils.EnvironmentUtils.NAIS_CLUSTER_NAME_PROPERTY_NAME
import no.nav.common.utils.SslUtils
import no.nav.henvendelse.consumer.kodeverk.generated.models.GetKodeverkKoderBetydningerResponseDTO
import no.nav.henvendelse.consumer.pdl.generated.HentAktorId
import no.nav.henvendelse.consumer.sfhenvendelse.generated.models.HenvendelseDTO
import no.nav.henvendelse.consumer.sfhenvendelse.generated.models.MeldingDTO
import no.nav.henvendelse.consumer.sfhenvendelse.generated.models.MeldingFraDTO
import no.nav.henvendelse.service.kodeverk.Kodeverk
import no.nav.henvendelse.utils.JacksonUtils
import org.springframework.boot.SpringApplication
import java.time.OffsetDateTime
import java.util.*

fun main(args: Array<String>) {
    System.setProperty(NAIS_CLUSTER_NAME_PROPERTY_NAME, "dev-fss")
    System.setProperty(SERVICEUSER_USERNAME_PROPERTY, "dummy")
    System.setProperty(SERVICEUSER_PASSWORD_PROPERTY, "dummy")

    loadFromYaml(".nais/preprod.yaml")
    SslUtils.setupTruststore()
    SSLTestUtils.disableCertificateChecks()

    startExternalMocks()

    val application = SpringApplication(Application::class.java)
    application.setAdditionalProfiles("local")
    application.run(*args)
}

private fun startExternalMocks(): WireMockServer {
    val server = WireMockServer()
    server.start()
    server.also(::addStsMock)
    server.also(::addPdlMock)
    server.also(::addKodeverkMock)
    server.also(::addSalesforceMock)
    return server
}

private fun addStsMock(server: WireMockServer) {
    val oidcConfig = JacksonUtils.toJson(
        mapOf(
            "token_endpoint" to "http://localhost:${server.port()}/token"
        )
    )
    val accessToken = JacksonUtils.toJson(
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

    val body = JacksonUtils.toJson(
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
        post(urlPathEqualTo("/pdl/graphql"))
            .willReturn(aResponse().withStatus(200).withBody(body))
    )
    System.setProperty("PDL_URL", "http://localhost:${server.port()}/pdl/graphql")
}

private fun addKodeverkMock(server: WireMockServer) {
    val response = aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "application/json")
        .withBody(JacksonUtils.toJson(GetKodeverkKoderBetydningerResponseDTO(emptyMap())))

    for (kodeverk in Kodeverk.values()) {
        server.stubFor(
            get(urlPathEqualTo("/api/v1/kodeverk/${kodeverk.kodeverknavn}/koder/betydninger"))
                .willReturn(response)
        )
    }

    System.setProperty("KODEVERK_URL", "http://localhost:${server.port()}")
}

private fun addSalesforceMock(server: WireMockServer) {
    val kodeverkResponse = aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "application/json")
        .withBody(JacksonUtils.toJson(emptyList<String>()))

    server.stubFor(
        get(urlPathEqualTo("/henvendelse/kodeverk/temagrupper"))
            .willReturn(kodeverkResponse)
    )

    val henvendelse = HenvendelseDTO(
        kjedeId = "ABBA1001",
        henvendelseType = HenvendelseDTO.HenvendelseType.SAMTALEREFERAT,
        fnr = "12345679810",
        aktorId = "987654321987",
        opprettetDato = OffsetDateTime.now(),
        kontorsperre = false,
        feilsendt = false,
        meldinger = listOf(
            MeldingDTO(
                sendtDato = OffsetDateTime.now(),
                fra = MeldingFraDTO(
                    ident = "12345678910",
                    identType = MeldingFraDTO.IdentType.AKTORID
                ),
                fritekst = "Noe random innhold"
            )
        )
    )

    val henvendelseResponse = aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "application/json")
        .withBody(
            JacksonUtils.toJson(
                listOf(
                    henvendelse
                )
            )
        )

    server.stubFor(
        get(urlPathEqualTo("/henvendelseinfo/henvendelseliste"))
            .willReturn(henvendelseResponse)
    )

    System.setProperty("SF_HENVENDELSE_URL", "http://localhost:${server.port()}")
}
