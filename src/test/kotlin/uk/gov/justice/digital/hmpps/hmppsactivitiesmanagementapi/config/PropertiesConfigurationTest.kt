package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.Properties

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [PropertiesConfiguration::class])
class PropertiesConfigurationTest {

  @Autowired
  private lateinit var context: ApplicationContext

  @Autowired
  @Qualifier("whereaboutsGroups")
  private lateinit var properties: Properties

  @Test
  fun checkContext() {
    assertThat(context).isNotNull
  }

  @Test
  fun groupsPropertiesWiredInUsingQualifier() {
    assertThat(properties)
      .isNotEmpty
      .containsKeys("MDI_Houseblock 1", "HEI_Segregation Unit")
  }

  @Test
  fun whereaboutsGroups_AreAllPatternsThatCompile() {
    properties.values.flatMap { (it as String).split(",") }.map { Regex(it).matches("some input") }
  }

  @Test
  fun whereaboutsGroups_NoDuplicateValues() {
    val duplicates =
      properties.values.flatMap { (it as String).split(",") }.groupingBy { it }.eachCount().any { it.value > 1 }
    assertThat(duplicates).isFalse
  }
}
