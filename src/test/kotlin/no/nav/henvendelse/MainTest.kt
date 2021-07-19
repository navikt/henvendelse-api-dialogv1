package no.nav.henvendelse

import no.nav.common.nais.NaisYamlUtils
import no.nav.common.test.ssl.SSLTestUtils
import no.nav.common.utils.SslUtils
import org.springframework.boot.SpringApplication

fun main(args: Array<String>) {
    loadVaultSecrets()
    NaisYamlUtils.loadFromYaml(NaisYamlUtils.getTemplatedConfig(".nais/preprod.yaml", mapOf("namespace" to "q0")))
    SslUtils.setupTruststore()
    SSLTestUtils.disableCertificateChecks()

    val application = SpringApplication(Application::class.java)
    application.setAdditionalProfiles("local")
    application.run(*args)
}

private fun loadVaultSecrets() {
    System.setProperty(SERVICEUSER_USERNAME_PROPERTY, "dummy")
    System.setProperty(SERVICEUSER_PASSWORD_PROPERTY, "dummy")
}
