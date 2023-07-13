package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.filter

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Activity

@ControllerAdvice
class ActivityControllerResponseFilterAdvice : ResponseBodyAdvice<Activity> {

  companion object {
    val log = LoggerFactory.getLogger(this::class.java)
  }

  override fun beforeBodyWrite(
    activity: Activity?,
    returnType: MethodParameter,
    selectedContentType: MediaType,
    selectedConverterType: Class<out HttpMessageConverter<*>>,
    request: ServerHttpRequest,
    response: ServerHttpResponse,
  ): Activity? {
    val userCaseLoadId = request.headers.getFirst("Caseload-Id")
    var activityPrisonCode = activity?.prisonCode
    if (activityPrisonCode != userCaseLoadId) {
      log.error(
        "Cannot return Activity [${activity?.id}] from method [${returnType.method.name}] because the Activity " +
          "is for prison [$activityPrisonCode] and the user's case load is for prison [$userCaseLoadId].",
      )
      throw EntityNotFoundException()
    } else {
      return activity
    }
  }

  override fun supports(returnType: MethodParameter, converterType: Class<out HttpMessageConverter<*>>): Boolean {
    return returnType.parameterType.equals(Activity::class.java) && returnType.method.name.startsWith("get")
  }
}
