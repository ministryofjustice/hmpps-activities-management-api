package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config

import io.swagger.v3.core.util.PrimitiveType
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfiguration(buildProperties: BuildProperties) {
  private val version: String = buildProperties.version

  @Bean
  fun customOpenAPI(buildProperties: BuildProperties): OpenAPI? = OpenAPI()
    .components(
      Components().addSecuritySchemes(
        "bearer-jwt",
        SecurityScheme()
          .type(SecurityScheme.Type.HTTP)
          .scheme("bearer")
          .bearerFormat("JWT")
          .`in`(SecurityScheme.In.HEADER)
          .name("Authorization")
      )
    )
    .info(
      Info()
        .title("Activities management API")
        .version(version)
        .description("API for management of prisoner activities")
        .contact(
          Contact()
            .name("HMPPS Digital Studio")
            .email("feedback@digital.justice.gov.uk")
        )
    )
    .addSecurityItem(SecurityRequirement().addList("bearer-jwt", listOf("read", "write")))


  @Bean
  fun enableLocalTimePrimitiveType() {
    PrimitiveType.enablePartialTime()
  }
}
