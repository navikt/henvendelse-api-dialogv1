package no.nav.henvendelse

import no.nav.common.utils.EnvironmentUtils
import no.nav.common.utils.EnvironmentUtils.Type.PUBLIC
import no.nav.common.utils.EnvironmentUtils.Type.SECRET
import no.nav.common.utils.NaisUtils
import no.nav.common.utils.SslUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class Application

val log: Logger = LoggerFactory.getLogger(Application::class.java)
fun main(args: Array<String>) {
    // Java 9+ krever avhengigheten jaxws-ri som bruker deprecated service mechanism (javax.xml.soap.MetaFactory). Overrider den manuelt her.
    System.setProperty("javax.xml.soap.SAAJMetaFactory", "com.sun.xml.messaging.saaj.soap.SAAJMetaFactoryImpl")

    loadVaultSecrets()
    SslUtils.setupTruststore()
    runApplication<Application>(*args)
}

fun loadVaultSecrets() {
    val serviceUser = NaisUtils.getCredentials("service_user")
    EnvironmentUtils.setProperty(SERVICEUSER_USERNAME_PROPERTY, serviceUser.username, PUBLIC)
    EnvironmentUtils.setProperty(SERVICEUSER_PASSWORD_PROPERTY, serviceUser.password, SECRET)
}
