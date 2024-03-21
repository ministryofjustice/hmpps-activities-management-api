import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintFormatTask
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "5.15.3"
  kotlin("plugin.spring") version "1.9.22"
  kotlin("plugin.jpa") version "1.9.22"
  jacoco
  id("org.openapi.generator") version "7.3.0"
  id("io.sentry.jvm.gradle") version "4.3.0"
}

allOpen {
  annotations(
    "javax.persistence.Entity",
    "javax.persistence.MappedSuperclass",
    "javax.persistence.Embeddable",
  )
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:0.2.2")

  // Spring boot dependencies
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:3.1.1")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

  // Spring framework retryable dependencies
  implementation("org.springframework.retry:spring-retry")
  implementation("org.springframework:spring-aspects")

  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:1.32.0")

  // OpenAPI
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")

  // Other dependencies
  implementation("org.apache.commons:commons-text:1.11.0")

  // Database dependencies
  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.postgresql:postgresql:42.7.2")

  // Test dependencies
  testImplementation("org.wiremock:wiremock-standalone:3.3.1")
  testImplementation("com.h2database:h2")
  testImplementation("io.jsonwebtoken:jjwt-impl:0.12.5")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.12.5")
  testImplementation("org.mockito:mockito-inline:5.2.0")
  testImplementation("net.javacrumbs.json-unit:json-unit:3.2.4")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:3.2.4")
  testImplementation("net.javacrumbs.json-unit:json-unit-json-path:3.2.4")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.awaitility:awaitility-kotlin")
}

kotlin {
  jvmToolchain(21)
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    dependsOn("buildPrisonApiModel", "buildNonAssociationsApiModel", "buildIncentivesApiModel", "copyPreCommitHook")
    kotlinOptions {
      jvmTarget = "21"
    }
  }
  withType<KtLintCheckTask> {
    // Under gradle 8 we must declare the dependency here, even if we're not going to be linting the model
    mustRunAfter("buildPrisonApiModel", "buildNonAssociationsApiModel", "buildIncentivesApiModel")
  }
  withType<KtLintFormatTask> {
    // Under gradle 8 we must declare the dependency here, even if we're not going to be linting the model
    mustRunAfter("buildPrisonApiModel", "buildNonAssociationsApiModel", "buildIncentivesApiModel")
  }
}

val configValues = mapOf(
  "dateLibrary" to "java8-localdatetime",
  "serializationLibrary" to "jackson",
  "useBeanValidation" to "false",
  "enumPropertyNaming" to "UPPERCASE",
)

val buildDirectory: Directory = layout.buildDirectory.get()

tasks.register("buildPrisonApiModel", GenerateTask::class) {
  generatorName.set("kotlin-spring")
  inputSpec.set("openapi-specs/prison-api.json")
  outputDir.set("$buildDirectory/generated/prisonapi")
  modelPackage.set("uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model")
  configOptions.set(configValues)
  globalProperties.set(mapOf("models" to ""))
}

tasks.register("buildNonAssociationsApiModel", GenerateTask::class) {
  generatorName.set("kotlin-spring")
  inputSpec.set("openapi-specs/non-associations-api.json")
  outputDir.set("$buildDirectory/generated/nonassociations")
  modelPackage.set("uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nonassociationsapi.model")
  configOptions.set(configValues)
  globalProperties.set(mapOf("models" to ""))
}

tasks.register("buildIncentivesApiModel", GenerateTask::class) {
  generatorName.set("kotlin-spring")
  inputSpec.set("openapi-specs/incentives-api.json")
  outputDir.set("$buildDirectory/generated/incentivesapi")
  modelPackage.set("uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.incentivesapi.model")
  configOptions.set(configValues)
  globalProperties.set(mapOf("models" to ""))
}

tasks.register("copyPreCommitHook", Copy::class) {
  from(project.file("pre-commit"))
  into(project.file(".git/hooks"))
  setFileMode(0b111101101)
  dependsOn("generateGitProperties")
}

val generatedProjectDirs = listOf("prisonapi", "nonassociations", "incentivesapi")

kotlin {
  generatedProjectDirs.forEach { generatedProject ->
    sourceSets["main"].apply {
      kotlin.srcDir("$buildDirectory/generated/$generatedProject/src/main/kotlin")
    }
  }
}
jacoco {
  toolVersion = "0.8.9"
}

val integrationTest = task<Test>("integrationTest") {
  description = "Integration tests"
  group = "verification"
  shouldRunAfter("test")
  // required for jjwt 0.12 - see https://github.com/jwtk/jjwt/issues/849
  jvmArgs("--add-exports", "java.base/sun.security.util=ALL-UNNAMED")
}

tasks.named<Test>("integrationTest") {
  useJUnitPlatform()
  filter {
    includeTestsMatching("*.integration.*")
  }
  maxHeapSize = "1024m"
}

tasks.named<Test>("test") {
  filter {
    excludeTestsMatching("*.integration.*")
  }
  finalizedBy("jacocoTestReport")
}

tasks.named<JacocoReport>("jacocoTestReport") {
  reports {
    xml.required.set(true)
    html.required.set(true)
  }
}

configure<KtlintExtension> {
  filter {
    generatedProjectDirs.forEach { generatedProject ->
      exclude { element ->
        element.file.path.contains("build/generated/$generatedProject/src/main/")
      }
    }
    exclude {
      it.file.path.contains("prisonersearchapi/model/")
    }
    exclude {
      it.file.path.contains("prisonapi/overrides/")
    }
  }
}
