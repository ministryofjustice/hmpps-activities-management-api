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

  @Autowired
  @Qualifier("externalActivitiesEnabledPrisons")
  private lateinit var prisons: Properties

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

  @Test
  fun prisonsPropertiesWiredInUsingQualifier() {
    assertThat(prisons)
      .isNotEmpty
      .containsKeys("AGI", "KMI", "SUI")
  }

  @Test
  fun prisonsProperties_ContainsExpectedPrisonNames() {
    assertThat(prisons.getProperty("AGI")).isEqualTo("Askham Grange")
    assertThat(prisons.getProperty("KMI")).isEqualTo("Kirkham")
    assertThat(prisons.getProperty("SUI")).isEqualTo("Sudbury")
  }

  @Test
  fun prisonsProperties_AllEntriesHaveNonBlankCodeAndName() {
    prisons.entries.forEach { entry ->
      assertThat(entry.key as String).isNotBlank()
      assertThat(entry.value as String).isNotBlank()
    }
  }

  @Test
  fun prisonsProperties_NoDuplicateNames() {
    val duplicates = prisons.values.groupingBy { it }.eachCount().any { it.value > 1 }
    assertThat(duplicates).isFalse
  }
}
