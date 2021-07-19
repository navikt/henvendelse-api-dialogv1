package no.nav.henvendelse.service

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import no.nav.common.cxf.saml.SamlUtils
import no.nav.common.utils.EnvironmentUtils
import no.nav.henvendelse.SECURITY_TOKEN_SERVICE_TOKEN_URL_PROPERTY
import no.nav.henvendelse.SERVICEUSER_PASSWORD_PROPERTY
import no.nav.henvendelse.SERVICEUSER_USERNAME_PROPERTY
import org.apache.cxf.message.Message
import org.apache.cxf.rt.security.saml.claims.SAMLSecurityContext
import org.apache.cxf.security.SecurityContext
import org.apache.wss4j.common.principal.SAMLTokenPrincipal
import org.joda.time.DateTime
import org.opensaml.saml.saml2.core.Assertion
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import java.io.StringWriter
import java.util.*
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

@Service
class SamlToOidcService {
    private val tokenCache: MutableMap<String, String> = HashMap()
    private val logger = LoggerFactory.getLogger(SamlToOidcService::class.java)
    private val stsUrl = EnvironmentUtils.getRequiredProperty(SECURITY_TOKEN_SERVICE_TOKEN_URL_PROPERTY)

    fun konverterSamlTokenTilOIDCToken(currentMessage: Message): String {
        val consumerId = getConsumerId(currentMessage)
        val cachedToken = tokenCache[consumerId]

        if (cachedToken != null) {
            if (tokenNotExpired(cachedToken)) {
                return cachedToken
            } else {
                tokenCache.remove(consumerId)
            }
        }

        val oidcToken: JsonElement = getOidcToken(currentMessage)
        if (consumerIdFunnet(consumerId)) {
            tokenCache[consumerId] = oidcToken.asString
        }
        return oidcToken.asString
    }

    private fun consumerIdFunnet(consumerId: String): Boolean {
        return CONSUMER_ID_IKKE_FUNNET != consumerId
    }

    private fun getOidcToken(currentMessage: Message): JsonElement {
        val samlAssertion = getSamlAssertion(currentMessage)
        val encodedSamlToken = Base64.getUrlEncoder().encodeToString(samlAssertion.toByteArray())
        val tokenResponse = getOidcTokenFromSamlToken(encodedSamlToken)

        val jsonParser = JsonParser()
        val jsonResponse: JsonElement = jsonParser.parse(tokenResponse!!.body)
        val oidcToken: JsonElement? = jsonResponse.asJsonObject.get(ACCESS_TOKEN_URI)

        if (oidcToken == null || oidcToken.getAsString().isEmpty()) {
            throw RuntimeException("Har ikke fått OIDC-token, OIDC-token er: $oidcToken")
        }
        return oidcToken
    }

    private fun tokenNotExpired(cachedToken: String): Boolean {
        val jsonParser = JsonParser()
        val base64encodedToken = cachedToken.split("\\.".toRegex()).toTypedArray()[1]

        val oidcToken: JsonElement = jsonParser.parse(String(Base64.getUrlDecoder().decode(base64encodedToken)))
        val expiry: JsonElement = oidcToken.getAsJsonObject().get(EXPIRARY_URI)

        val nowPlus5mins = DateTime.now().plusMinutes(5)

        return nowPlus5mins.millis < expiry.getAsLong() * 1000
    }

    private fun getConsumerId(currentMessage: Message): String {
        val samlSecurityContext = currentMessage[SecurityContext::class.java.name] as SAMLSecurityContext?
            ?: throw RuntimeException("Ugyldig SAML-token, finner ikke samlSecurityContext")
        val claims = samlSecurityContext.claims

        for (claim in claims) {
            if (SamlUtils.CONSUMER_ID == claim.claimType) {
                return claim.values[0].toString()
            }
        }

        logger.warn("ConsumerId ikke funnet, tokencache vil ikke fungere.")
        return CONSUMER_ID_IKKE_FUNNET
    }

    private fun getOidcTokenFromSamlToken(encodedSamlToken: String): ResponseEntity<String>? {
        val restTemplate = RestTemplate()

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED

        val systemUsername = EnvironmentUtils.getRequiredProperty(SERVICEUSER_USERNAME_PROPERTY)
        val systemPassword = EnvironmentUtils.getRequiredProperty(SERVICEUSER_PASSWORD_PROPERTY)
        val auth: String = "$systemUsername:$systemPassword"

        val encodedAuth = "Basic " + Base64.getEncoder().encodeToString(auth.toByteArray())
        headers["Authorization"] = encodedAuth

        val map: MultiValueMap<String, String> = LinkedMultiValueMap()
        map.add("grant_type", GRANT_TYPE_PARAM)
        map.add("requested_token_type", REQUESTED_TOKEN_TYPE_PARAM)
        map.add("subject_token_type", SUBJECT_TOKEN_TYPE_PARAM)
        map.add("subject_token", encodedSamlToken)

        val request = HttpEntity(map, headers)
        var tokenResponse: ResponseEntity<String>? = null
        try {
            tokenResponse = restTemplate.postForEntity("$stsUrl/exchange", request, String::class.java)
        } catch (e: RestClientException) {
            throw RuntimeException("Feilet i henting av OIDC-token", e)
        }
        return tokenResponse
    }

    private fun getSamlAssertion(currentMessage: Message): String {
        val sc = currentMessage[SecurityContext::class.java.name] as? SecurityContext
            ?: throw RuntimeException("Cannot get SecurityContext from SoapMessage")
        val samlTokenPrincipal = sc.userPrincipal as? SAMLTokenPrincipal
            ?: throw RuntimeException("Cannot get SAMLTokenPrincipal from SecurityContext")
        val assertion = samlTokenPrincipal.token.saml2

        return try {
            getSamlAssertionAsString(assertion)
        } catch (e: TransformerException) {
            throw RuntimeException("Klarte ikke finne SAML-assertion", e)
        }
    }

    @Throws(TransformerException::class)
    private fun getSamlAssertionAsString(assertion: Assertion): String {
        val writer = StringWriter()
        val transformerFactory = TransformerFactory.newInstance()
        val transformer = transformerFactory.newTransformer()
        transformer.transform(DOMSource(assertion.dom), StreamResult(writer))
        return writer.toString()
    }

    companion object {
        private const val EXPIRARY_URI = "exp"
        private const val ACCESS_TOKEN_URI = "access_token"
        private const val CONSUMER_ID_IKKE_FUNNET = ""

        private const val GRANT_TYPE_PARAM = "urn:ietf:params:oauth:grant-type:token-exchange"
        private const val REQUESTED_TOKEN_TYPE_PARAM = "urn:ietf:params:oauth:token-type:access_token"
        private const val SUBJECT_TOKEN_TYPE_PARAM = "urn:ietf:params:oauth:token-type:saml2"
    }
}
