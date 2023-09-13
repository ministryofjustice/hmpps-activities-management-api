package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSet
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSetDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentSetRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.checkCaseloadAccess

@Service
@Transactional(readOnly = true)
class AppointmentSetService(
  private val appointmentSetRepository: AppointmentSetRepository,
  private val referenceCodeService: ReferenceCodeService,
  private val locationService: LocationService,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val prisonApiClient: PrisonApiClient,
) {
  fun getAppointmentSetById(appointmentSetId: Long): AppointmentSet {
    val appointmentSet = appointmentSetRepository.findOrThrowNotFound(appointmentSetId)
    checkCaseloadAccess(appointmentSet.prisonCode)

    return appointmentSet.toModel()
  }

  fun getAppointmentSetDetailsById(appointmentSetId: Long): AppointmentSetDetails {
    val appointmentSet = appointmentSetRepository.findOrThrowNotFound(appointmentSetId)
    checkCaseloadAccess(appointmentSet.prisonCode)

    val prisonerMap = prisonerSearchApiClient.findByPrisonerNumbersMap(appointmentSet.prisonerNumbers())

    val referenceCodeMap = referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY)

    val locationMap = locationService.getLocationsForAppointmentsMap(appointmentSet.prisonCode)

    val userMap = prisonApiClient.getUserDetailsList(appointmentSet.usernames()).associateBy { it.username }

    return appointmentSet.toDetails(prisonerMap, referenceCodeMap, locationMap, userMap)
  }
}
