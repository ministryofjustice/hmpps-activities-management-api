plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "4.5.0"
  kotlin("plugin.spring") version "1.7.10"
  kotlin("plugin.jpa") version "1.7.10"
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
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:1.1.9")

  // Database dependencies
  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.postgresql:postgresql:42.5.0")

  // Test dependencies
  testImplementation("com.h2database:h2")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(18))
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "18"
    }
  }
}
