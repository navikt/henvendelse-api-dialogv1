package no.nav.henvendelse.config

import com.nimbusds.jwt.JWTParser
import no.nav.common.cxf.StsConfig
import no.nav.common.health.HealthCheckResult
import no.nav.common.health.selftest.SelfTestCheck
import no.nav.common.sts.NaisSystemUserTokenProvider
import no.nav.common.sts.SystemUserTokenProvider
import no.nav.common.utils.EnvironmentUtils
import no.nav.henvendelse.SERVICEUSER_PASSWORD_PROPERTY
import no.nav.henvendelse.SERVICEUSER_USERNAME_PROPERTY
import no.nav.henvendelse.log
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SecurityTokenServiceConfig {
    @Bean
    fun stsConfig(): StsConfig = StsConfig.builder()
        .url(EnvironmentUtils.getRequiredProperty("SECURITY_TOKEN_SERVICE_SAML_URL"))
        .username(EnvironmentUtils.getRequiredProperty(SERVICEUSER_USERNAME_PROPERTY))
        .password(EnvironmentUtils.getRequiredProperty(SERVICEUSER_PASSWORD_PROPERTY))
        .build()

    @Bean
    fun systemUserTokenProvider(): SystemUserTokenProvider = NaisSystemUserTokenProvider(
        EnvironmentUtils.getRequiredProperty("SECURITY_TOKEN_SERVICE_DISCOVERY_URL"),
        EnvironmentUtils.getRequiredProperty(SERVICEUSER_USERNAME_PROPERTY),
        EnvironmentUtils.getRequiredProperty(SERVICEUSER_PASSWORD_PROPERTY)
    )

    @Bean
    fun systemUserTokenProviderCheck(provider: SystemUserTokenProvider) = SelfTestCheck(
        "SystemUserTokenProvider",
        true
    ) {
        try {
            val jwtClaims = JWTParser
                .parse(provider.systemUserToken)
                .jwtClaimsSet
                .toJSONObject()
                .toJSONString()
            log.info("Got systemuser jwt\n$jwtClaims")
            HealthCheckResult.healthy()
        } catch (e: Exception) {
            HealthCheckResult.unhealthy(Exception("Kunne ikke hente ut STS token", e))
        }
    }
}
