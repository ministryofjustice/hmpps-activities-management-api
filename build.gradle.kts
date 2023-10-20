import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintFormatTask

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "5.5.0"
  kotlin("plugin.spring") version "1.9.10"
  kotlin("plugin.jpa") version "1.9.10"
  jacoco
  id("org.openapi.generator") version "6.6.0"
}

allOpen {
  annotations(
    "javax.persistence.Entity",
    "javax.persistence.MappedSuperclass",
    "javax.persistence.Embeddable"
  )
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  // Spring boot dependencies
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:2.1.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:1.25.0")

  // OpenAPI
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0")

  // Other dependencies
  implementation("org.apache.commons:commons-text:1.10.0")

  // Database dependencies
  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.postgresql:postgresql:42.6.0")

  // Test dependencies
  testImplementation("org.wiremock:wiremock:3.2.0")
  testImplementation("com.h2database:h2")
  testImplementation("io.jsonwebtoken:jjwt-impl:0.12.0")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.12.0")
  testImplementation("org.mockito:mockito-inline:5.2.0")
  testImplementation("net.javacrumbs.json-unit:json-unit:3.2.2")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:3.2.2")
  testImplementation("net.javacrumbs.json-unit:json-unit-json-path:3.2.2")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(18))
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    dependsOn("buildPrisonApiModel", "buildNonAssociationsApiModel")
    kotlinOptions {
      jvmTarget = "18"
    }
  }
  withType<KtLintCheckTask> {
    // Under gradle 8 we must declare the dependency here, even if we're not going to be linting the model
    mustRunAfter("buildPrisonApiModel", "buildNonAssociationsApiModel")
  }
  withType<KtLintFormatTask> {
    // Under gradle 8 we must declare the dependency here, even if we're not going to be linting the model
    mustRunAfter("buildPrisonApiModel", "buildNonAssociationsApiModel")
  }
}

val configValues = mapOf(
  "dateLibrary" to "java8-localdatetime",
  "serializationLibrary" to "jackson",
  "useBeanValidation" to "false",
  "enumPropertyNaming" to "UPPERCASE"
)

tasks.register("buildPrisonApiModel", org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
  generatorName.set("kotlin-spring")
  inputSpec.set("openapi-specs/prison-api.json")
  outputDir.set("$buildDir/generated")
  modelPackage.set("uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model")
  configOptions.set(configValues)
  globalProperties.set(mapOf("models" to ""))
}

tasks.register("buildNonAssociationsApiModel", org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
  generatorName.set("kotlin")
  inputSpec.set("openapi-specs/non-associations-api.json")
  outputDir.set("$buildDir/generated")
  modelPackage.set("uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nonassociationsapi.model")
  configOptions.set(configValues)
  globalProperties.set(mapOf("models" to ""))
}

kotlin {
  sourceSets["main"].apply {
    kotlin.srcDir("$buildDir/generated/src/main/kotlin")
  }
}

jacoco {
  toolVersion = "0.8.8"
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
    exclude {
      it.file.path.contains("build/generated/src/main/")
    }
    exclude {
      it.file.path.contains("prisonersearchapi/model/")
    }
    exclude {
      it.file.path.contains("prisonapi/overrides/")
    }
  }
}
