package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.containsExactly
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Method

class EndpointSecurityCheck {
  private data class EndpointInfo(val method: String, val hasEndpointLevelProtection: Boolean)

  private data class ControllerInfo(val controller: String, val unprotectedEndpoints: List<EndpointInfo>) {
    override fun toString() =
      "\n$controller:".plus(unprotectedEndpoints.joinToString(separator = "\n * ", prefix = "\n * ") { it.method })
  }

  @Test
  fun `Ensure checks are working by referencing fake unprotected controller`() {
    getAllUnprotectedControllers().map(ControllerInfo::controller) containsExactly listOf("class ${FakeUnprotectedController::class.qualifiedName}")
  }

  @Test
  fun `Ensure endpoints are checking roles`() {
    val controllers =
      getAllUnprotectedControllers().filterNot { it.controller == "class ${FakeUnprotectedController::class.qualifiedName}" }

    if (controllers.isNotEmpty()) {
      fail("Role checks missing in following locations: ${controllers.joinToString("\n")}\n")
    }
  }

  private fun getAllUnprotectedControllers() = ClassPathScanningCandidateComponentProvider(false)
    .apply { addIncludeFilter(AnnotationTypeFilter(RestController::class.java)) }
    .findCandidateComponents("uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi")
    .map { Class.forName(it.beanClassName) }
    .filterNot { it.isProtectedByAnnotation() }
    .map { ControllerInfo(it.toString(), it.getUnprotectedEndpoints()) }
    .filter { it.unprotectedEndpoints.isNotEmpty() }

  private fun Class<*>.getUnprotectedEndpoints() = methods
    .filter { it.isEndpoint() }
    .map { EndpointInfo(it.toString(), it.isProtectedByAnnotation()) }
    .filterNot(EndpointInfo::hasEndpointLevelProtection)

  private fun Method.isEndpoint() = this.annotations.any {
    it.annotationClass.qualifiedName!!.startsWith("org.springframework.web.bind.annotation")
  }

  private fun AnnotatedElement.isProtectedByAnnotation(): Boolean {
    if (ANNOTATIONS_THAT_DENOTE_EXCLUSION.any { this.isAnnotationPresent(it) }) return true
    val annotation = getAnnotation(PreAuthorize::class.java) ?: return false
    return annotation.value.contains("hasAnyRole") || annotation.value.contains("hasRole")
  }

  companion object {
    val ANNOTATIONS_THAT_DENOTE_EXCLUSION = setOf(ProtectedByIngress::class.java, PublicEndpoint::class.java)
  }
}

@RestController
@RequestMapping("/unprotected", produces = [MediaType.APPLICATION_JSON_VALUE])
internal class FakeUnprotectedController {
  @GetMapping
  fun getNothing(): Nothing = TODO()
}
