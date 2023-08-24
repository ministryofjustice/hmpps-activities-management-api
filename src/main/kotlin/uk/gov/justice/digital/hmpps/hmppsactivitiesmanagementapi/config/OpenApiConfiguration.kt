package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config

import io.swagger.v3.core.util.PrimitiveType
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.info.BuildProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.expression.BeanFactoryResolver
import org.springframework.expression.spel.SpelEvaluationException
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.method.HandlerMethod

@Configuration
class OpenApiConfiguration(buildProperties: BuildProperties) {
  private val version: String = buildProperties.version

  @Autowired
  private lateinit var context: ApplicationContext

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
          .name("Authorization"),
      ),
    )
    .info(
      Info()
        .title("Activities management API")
        .version(version)
        .description("API for management of prisoner activities")
        .contact(
          Contact()
            .name("HMPPS Digital Studio")
            .email("feedback@digital.justice.gov.uk"),
        ),
    )
    .addSecurityItem(SecurityRequirement().addList("bearer-jwt", listOf("read", "write")))

  @Bean
  fun preAuthorizeCustomizer(): OperationCustomizer {
    return OperationCustomizer { operation: Operation, handlerMethod: HandlerMethod ->
      // Get PreAuthorize for method or fallback to class annotation
      (
        handlerMethod.getMethodAnnotation(PreAuthorize::class.java)?.let {
          it.value
        } ?: handlerMethod.beanType.getAnnotation(PreAuthorize::class.java)?.let {
          it.value
        }
        )?.let {
        val preAuthExp = SpelExpressionParser().parseExpression(it)
        val spelEvalContext = StandardEvaluationContext()
        spelEvalContext.beanResolver = BeanFactoryResolver(context)
        spelEvalContext.setRootObject(
          object {
            fun hasRole(role: String) = listOf(role)
            fun hasAnyRole(vararg roles: String) = roles.toList()
          },
        )

        val roles = try { preAuthExp.getValue(spelEvalContext) as List<String> } catch (e: SpelEvaluationException) { emptyList() }
        if (roles.isNotEmpty()) {
          operation.description = "${operation.description ?: ""}\n\n" +
            "Requires one of the following roles:\n" +
            "${roles.joinToString(prefix = "* ", separator = "\n* ")}"
        }
      }

      operation
    }
  }

  @Bean
  fun enableLocalTimePrimitiveType() {
    PrimitiveType.enablePartialTime()
  }
}
