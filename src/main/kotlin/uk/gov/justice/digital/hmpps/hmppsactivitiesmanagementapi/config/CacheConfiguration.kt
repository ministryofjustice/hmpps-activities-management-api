package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.TimeUnit

@Configuration
@EnableCaching
@EnableScheduling
class CacheConfiguration {

  @Bean
  fun cacheManager(): CacheManager = ConcurrentMapCacheManager(BANK_HOLIDAYS_CACHE_NAME, PRISON_INCENTIVE_LEVELS_CACHE_NAME)

  @CacheEvict(value = [PRISON_INCENTIVE_LEVELS_CACHE_NAME], allEntries = true)
  @Scheduled(fixedDelay = TTL_HOURS_PRISONER_INCENTIVE_LEVELS, timeUnit = TimeUnit.DAYS)
  fun cacheEvictPrisonerIncentiveLevels() {
    log.info("Evicting cache: $PRISON_INCENTIVE_LEVELS_CACHE_NAME after $TTL_HOURS_PRISONER_INCENTIVE_LEVELS day")
  }

  @CacheEvict(value = [BANK_HOLIDAYS_CACHE_NAME], allEntries = true)
  @Scheduled(fixedDelay = TTL_HOURS_BANK_HOLIDAYS, timeUnit = TimeUnit.DAYS)
  fun cacheEvictBankHolidays() {
    log.info("Evicting cache: $BANK_HOLIDAYS_CACHE_NAME after $TTL_HOURS_BANK_HOLIDAYS days")
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
    const val BANK_HOLIDAYS_CACHE_NAME: String = "bankHolidays"
    const val PRISON_INCENTIVE_LEVELS_CACHE_NAME: String = "prisonIncentiveLevels"
    const val TTL_HOURS_PRISONER_INCENTIVE_LEVELS: Long = 1
    const val TTL_HOURS_BANK_HOLIDAYS: Long = 7
  }
}
