package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.UserDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentOccurrenceSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.UserSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Appointment as AppointmentEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrence as AppointmentOccurrenceEntity

@Service
class AppointmentDetailService(
  private val appointmentRepository: AppointmentRepository,
  private val locationService: LocationService,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val prisonApiClient: PrisonApiClient,
) {
  fun getAppointmentDetailById(appointmentId: Long): AppointmentDetail {
    val appointment = appointmentRepository.findOrThrowNotFound(appointmentId)

    val locationMap = locationService.getLocationsForAppointmentsMap(appointment.prisonCode, appointment.internalLocationIds())!!

    val userMap = prisonApiClient.getUserDetailsList(appointment.usernames()).block()!!.associateBy { it.username }

    val prisoners = prisonerSearchApiClient.findByPrisonerNumbers(appointment.prisonerNumbers()).block()!!

    return appointment.toDetail(locationMap, userMap, prisoners)
  }
}

private fun AppointmentEntity.toDetail(locationMap: Map<Long, Location>, userMap: Map<String, UserDetail>, prisoners: List<Prisoner>) =
  AppointmentDetail(
    this.appointmentId,
    this.category.toSummary(),
    this.prisonCode,
    if (this.inCell) {
      null
    } else {
      locationMap.getOrDefault(this.internalLocationId, null).toAppointmentLocationSummary(this.internalLocationId!!, this.prisonCode)
    },
    this.inCell,
    this.startDate,
    this.startTime,
    this.endTime,
    this.comment,
    this.created,
    userMap.getOrDefault(this.createdBy, null).toSummary(this.createdBy),
    this.updated,
    if (this.updatedBy == null) {
      null
    } else {
      userMap.getOrDefault(this.updatedBy, null).toSummary(this.updatedBy!!)
    },
    this.occurrences().toSummary(this.prisonCode, locationMap, userMap, this.comment),
    prisoners.toSummary(),
  )

private fun Location?.toAppointmentLocationSummary(locationId: Long, prisonCode: String) =
  if (this == null) {
    AppointmentLocationSummary(locationId, prisonCode, "UNKNOWN")
  } else {
    AppointmentLocationSummary(this.locationId, this.agencyId, this.userDescription ?: this.description)
  }

private fun UserDetail?.toSummary(username: String) =
  if (this == null) {
    UserSummary(-1, username, "UNKNOWN", "UNKNOWN")
  } else {
    UserSummary(this.staffId, this.username, this.firstName, this.lastName)
  }

private fun List<Prisoner>.toSummary() = map {
  PrisonerSummary(
    it.prisonerNumber,
    it.bookingId?.toLong() ?: -1,
    it.firstName,
    it.lastName,
    it.prisonId ?: "UNKNOWN",
    it.cellLocation ?: "UNKNOWN",
  )
}

private fun List<AppointmentOccurrenceEntity>.toSummary(prisonCode: String, locationMap: Map<Long, Location>, userMap: Map<String, UserDetail>, appointmentComment: String) = map {
  AppointmentOccurrenceSummary(
    it.appointmentOccurrenceId,
    if (it.inCell) null else locationMap.getOrDefault(it.internalLocationId, null).toAppointmentLocationSummary(it.internalLocationId!!, prisonCode),
    it.inCell,
    it.startDate,
    it.startTime,
    it.endTime,
    it.comment ?: appointmentComment,
    isEdited = false,
    isCancelled = false,
    updated = it.updated,
    updatedBy = if (it.updatedBy == null) null else userMap.getOrDefault(it.updatedBy, null).toSummary(it.updatedBy!!),
    prisonerCount = it.prisonerCount(),
  )
}
