package no.nav.henvendelse.consumer.sfhenvendelse

import no.nav.common.health.selftest.SelfTestCheck
import no.nav.henvendelse.service.dialog.SfDialogService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SfDialogConfig {
    @Bean
    fun sfDialogService() = SfDialogService()

    @Bean
    fun sfDialogServiceSelfTestCheck(sfDialogService: SfDialogService): SelfTestCheck = sfDialogService.selftestCheck
}
