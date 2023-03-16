package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.UserDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentOccurrenceDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentOccurrenceSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toAppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toSummary
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentOccurrence as AppointmentOccurrenceModel

@Entity
@Table(name = "appointment_occurrence")
data class AppointmentOccurrence(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val appointmentOccurrenceId: Long = 0,

  @ManyToOne
  @JoinColumn(name = "appointment_id", nullable = false)
  val appointment: Appointment,

  val sequenceNumber: Int,

  var internalLocationId: Long?,

  var inCell: Boolean,

  var startDate: LocalDate,

  var startTime: LocalTime,

  var endTime: LocalTime?,

  var comment: String? = null,

  var cancelled: Boolean = false,

  var updated: LocalDateTime? = null,

  var updatedBy: String? = null,
) {

  @OneToMany(mappedBy = "appointmentOccurrence", fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
  @Fetch(FetchMode.SUBSELECT)
  private val allocations: MutableList<AppointmentOccurrenceAllocation> = mutableListOf()

  @OneToMany(mappedBy = "appointmentOccurrence", fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
  @Fetch(FetchMode.SUBSELECT)
  private val instances: MutableList<AppointmentInstance> = mutableListOf()

  fun allocations() = allocations.toList()

  fun addAllocation(allocation: AppointmentOccurrenceAllocation) = allocations.add(allocation)

  fun instances() = instances.toList()

  fun addInstance(instance: AppointmentInstance) = instances.add(instance)

  fun prisonerNumbers() = allocations().map { allocation -> allocation.prisonerNumber }.distinct()

  fun prisonerCount() = prisonerNumbers().count()

  fun toModel() = AppointmentOccurrenceModel(
    id = appointmentOccurrenceId,
    internalLocationId = internalLocationId,
    inCell = inCell,
    startDate = startDate,
    startTime = startTime,
    endTime = endTime,
    comment = comment,
    cancelled = cancelled,
    updated = updated,
    updatedBy = updatedBy,
    allocations = allocations.toModel(),
    instances = instances.toModel(),
  )

  fun toSummary(prisonCode: String, locationMap: Map<Long, Location>, userMap: Map<String, UserDetail>, appointmentComment: String) =
    AppointmentOccurrenceSummary(
      appointmentOccurrenceId,
      sequenceNumber,
      if (inCell) null else locationMap.getOrDefault(internalLocationId, null).toAppointmentLocationSummary(internalLocationId!!, prisonCode),
      inCell,
      startDate,
      startTime,
      endTime,
      comment ?: appointmentComment,
      isEdited = false,
      isCancelled = false,
      updated = updated,
      updatedBy?.let { userMap.getOrDefault(updatedBy, null).toSummary(updatedBy!!) },
      prisonerCount = prisonerCount(),
    )

  fun toDetails(categorySummary: AppointmentCategorySummary, prisonCode: String, locationMap: Map<Long, Location>, userMap: Map<String, UserDetail>, prisoners: List<Prisoner>) =
    AppointmentOccurrenceDetails(
      appointmentOccurrenceId,
      appointment.appointmentId,
      sequenceNumber,
      categorySummary,
      prisonCode,
      if (inCell) {
        null
      } else {
        locationMap.getOrDefault(internalLocationId, null).toAppointmentLocationSummary(internalLocationId!!, prisonCode)
      },
      inCell,
      startDate,
      startTime,
      endTime,
      comment ?: appointment.comment,
      false,
      false,
      appointment.created,
      userMap.getOrDefault(appointment.createdBy, null).toSummary(appointment.createdBy),
      updated,
      if (updatedBy == null) {
        null
      } else {
        userMap.getOrDefault(updatedBy, null).toSummary(updatedBy!!)
      },
      prisoners.toSummary(),
    )
}

fun List<AppointmentOccurrence>.toModel() = map { it.toModel() }

fun List<AppointmentOccurrence>.toSummary(
  prisonCode: String,
  locationMap: Map<Long, Location>,
  userMap: Map<String, UserDetail>,
  appointmentComment: String,
) = map { it.toSummary(prisonCode, locationMap, userMap, appointmentComment) }
