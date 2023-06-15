package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.BulkAppointmentDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.BulkAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound

@Service
@Transactional(readOnly = true)
class BulkAppointmentDetailsService(
  private val bulkAppointmentRepository: BulkAppointmentRepository,
  private val referenceCodeService: ReferenceCodeService,
  private val locationService: LocationService,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val prisonApiClient: PrisonApiClient,
) {
  fun getBulkAppointmentDetailsById(appointmentId: Long): BulkAppointmentDetails {
    val bulkAppointment = bulkAppointmentRepository.findOrThrowNotFound(appointmentId)

    val prisonerMap = prisonerSearchApiClient.findByPrisonerNumbersMap(bulkAppointment.prisonerNumbers())

    val referenceCodeMap = referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY)

    val locationMap = locationService.getLocationsForAppointmentsMap(bulkAppointment.prisonCode())

    val userMap = prisonApiClient.getUserDetailsList(bulkAppointment.usernames()).associateBy { it.username }

    return bulkAppointment.toDetails(prisonerMap, referenceCodeMap, locationMap, userMap)
  }
}
