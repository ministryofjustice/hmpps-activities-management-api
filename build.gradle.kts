
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "10.0.3"
  kotlin("plugin.spring") version "2.3.0"
  kotlin("plugin.jpa") version "2.3.0"
  jacoco
  id("org.openapi.generator") version "7.19.0"
  id("io.sentry.jvm.gradle") version "6.0.0"
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
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:2.0.0")

  // Spring boot dependencies
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-webclient")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.data:spring-data-envers:4.0.2")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:7.0.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.10.2")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.10.2")
  implementation("org.springframework.boot:spring-boot-starter-flyway")
  implementation("org.springframework.boot:spring-boot-jackson2")

  // Spring framework retryable dependencies
  implementation("org.springframework.retry:spring-retry")
  implementation("org.springframework:spring-aspects")

  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.24.0")

  // OpenAPI
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.1")

  // AWS
  implementation("com.squareup.okhttp3:okhttp:5.3.2") {
    version {
      strictly("5.0.0-alpha.14")
    }
  }

  implementation("aws.sdk.kotlin:s3:1.6.9")

  // Other dependencies
  implementation("org.apache.commons:commons-text:1.15.0")

  // Database dependencies
  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql")

  // Test dependencies
  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:2.0.0")
  testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
  testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
  testImplementation("org.wiremock:wiremock-standalone:3.13.2")
  testImplementation("org.springframework.boot:spring-boot-testcontainers")
  testImplementation("org.testcontainers:postgresql:1.21.4")
  testImplementation("org.testcontainers:localstack:1.21.4")
  testImplementation("io.jsonwebtoken:jjwt-impl:0.13.0")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.13.0")
  testImplementation("org.mockito:mockito-inline:5.2.0")
  testImplementation("net.javacrumbs.json-unit:json-unit:5.1.0")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:5.1.0")
  testImplementation("net.javacrumbs.json-unit:json-unit-json-path:5.1.0")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.awaitility:awaitility-kotlin")
  testImplementation("org.skyscreamer:jsonassert")
  testImplementation("io.mockk:mockk:1.14.9")
}

kotlin {
  jvmToolchain(25)
  compilerOptions {
    freeCompilerArgs.addAll("-Xannotation-default-target=param-property")
  }
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    dependsOn("buildPrisonApiModel", "buildNonAssociationsApiModel", "buildIncentivesApiModel", "buildLocationsInsidePrisonApiModel")
    compilerOptions.jvmTarget = JvmTarget.JVM_25
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

tasks.register("buildLocationsInsidePrisonApiModel", GenerateTask::class) {
  generatorName.set("kotlin")
  inputSpec.set("openapi-specs/locations-inside-prison-api.json")
  outputDir.set("$buildDirectory/generated/locationsinsideprisonapi")
  modelPackage.set("uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.model")
  configOptions.set(configValues)
  globalProperties.set(mapOf("models" to ""))
}

tasks.register("listrepos") {
  doLast {
    println("Repositories:")
    project.repositories.map { it as MavenArtifactRepository }
      .forEach {
        println("Name: ${it.name}; url: ${it.url}")
      }
  }
}

val generatedProjectDirs = listOf("prisonapi", "nonassociations", "incentivesapi", "locationsinsideprisonapi")

kotlin {
  generatedProjectDirs.forEach { generatedProject ->
    sourceSets["main"].apply {
      kotlin.srcDir("$buildDirectory/generated/$generatedProject/src/main/kotlin")
    }
  }
}
jacoco {
  toolVersion = "0.8.14"
}

tasks.register("integrationTest", Test::class) {
  description = "Runs integration tests"
  group = "verification"
  testClassesDirs = sourceSets["test"].output.classesDirs
  classpath = sourceSets["test"].runtimeClasspath

  useJUnitPlatform {
    filter {
      includeTestsMatching("*.integration.*")
    }
  }

  shouldRunAfter("test")
  maxHeapSize = "2048m"
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

tasks.named("runKtlintCheckOverMainSourceSet") {
  dependsOn("buildPrisonApiModel")
  dependsOn("buildIncentivesApiModel")
  dependsOn("buildNonAssociationsApiModel")
  dependsOn("buildLocationsInsidePrisonApiModel")
}

ktlint {
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
