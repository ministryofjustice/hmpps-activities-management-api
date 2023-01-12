plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "4.8.1-beta-1"
  kotlin("plugin.spring") version "1.8.0"
  kotlin("plugin.jpa") version "1.8.0"
  jacoco
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
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:1.2.0")

  // OpenAPI
  implementation("org.springdoc:springdoc-openapi-ui:1.6.14")
  implementation("org.springdoc:springdoc-openapi-data-rest:1.6.14")
  implementation("org.springdoc:springdoc-openapi-kotlin:1.6.14")

  // Other dependencies
  implementation("org.apache.commons:commons-text:1.10.0")

  // Database dependencies
  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.postgresql:postgresql:42.5.1")

  // Test dependencies
  testImplementation("com.github.tomakehurst:wiremock-standalone:2.27.2")
  testImplementation("com.h2database:h2")
  testImplementation("io.jsonwebtoken:jjwt:0.9.1")
  testImplementation("org.mockito:mockito-inline:4.11.0")
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

jacoco {
  toolVersion = "0.8.8"
}

val integrationTest = task<Test>("integrationTest") {
  description = "Integration tests"
  group = "verification"
  shouldRunAfter("test")
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
