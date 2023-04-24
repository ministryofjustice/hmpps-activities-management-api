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
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import org.hibernate.annotations.Where
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.UserDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentOccurrenceDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentOccurrenceSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toAppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toAppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toSummary
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentOccurrence as AppointmentOccurrenceModel

@Entity
@Table(name = "appointment_occurrence")
@Where(clause = "deleted = false")
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

  var updated: LocalDateTime? = null,

  var updatedBy: String? = null,

  var deleted: Boolean = false,
) {

  var cancelled: LocalDateTime? = null

  @OneToOne
  @JoinColumn(name = "cancellation_reason_id")
  var cancellationReason: AppointmentCancellationReason? = null

  var cancelledBy: String? = null

  @OneToMany(mappedBy = "appointmentOccurrence", fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
  @Fetch(FetchMode.SUBSELECT)
  private val allocations: MutableList<AppointmentOccurrenceAllocation> = mutableListOf()

  fun allocations() = allocations.toList()

  fun addAllocation(allocation: AppointmentOccurrenceAllocation) {
    failIfIndividualAppointmentAlreadyAllocated()
    allocations.add(allocation)
  }

  fun removeAllocation(allocation: AppointmentOccurrenceAllocation) = allocations.remove(allocation)

  fun prisonerNumbers() = allocations().map { allocation -> allocation.prisonerNumber }.distinct()

  fun prisonerCount() = prisonerNumbers().count()

  fun isCancelled() = cancellationReason?.isDelete == false

  fun isDeleted() = cancellationReason?.isDelete == true

  fun toModel() = AppointmentOccurrenceModel(
    id = appointmentOccurrenceId,
    internalLocationId = internalLocationId,
    inCell = inCell,
    startDate = startDate,
    startTime = startTime,
    endTime = endTime,
    comment = comment,
    cancelled = cancelled,
    cancellationReasonId = cancellationReason?.appointmentCancellationReasonId,
    cancelledBy = cancelledBy,
    updated = updated,
    updatedBy = updatedBy,
    allocations = allocations.toModel(),
  )

  fun toSummary(prisonCode: String, locationMap: Map<Long, Location>, userMap: Map<String, UserDetail>, appointmentComment: String) =
    AppointmentOccurrenceSummary(
      appointmentOccurrenceId,
      sequenceNumber,
      if (inCell) null else locationMap[internalLocationId].toAppointmentLocationSummary(internalLocationId!!, prisonCode),
      inCell,
      startDate,
      startTime,
      endTime,
      comment ?: appointmentComment,
      isEdited = false,
      isCancelled = isCancelled(),
      updated = updated,
      updatedBy?.let { userMap[updatedBy].toSummary(updatedBy!!) },
      prisonerCount = prisonerCount(),
    )

  fun toDetails(referenceCodeMap: Map<String, ReferenceCode>, prisonCode: String, locationMap: Map<Long, Location>, userMap: Map<String, UserDetail>, prisoners: List<Prisoner>) =
    AppointmentOccurrenceDetails(
      appointmentOccurrenceId,
      appointment.appointmentId,
      appointment.appointmentType,
      sequenceNumber,
      referenceCodeMap[appointment.categoryCode].toAppointmentCategorySummary(appointment.categoryCode),
      prisonCode,
      if (inCell) {
        null
      } else {
        locationMap[internalLocationId].toAppointmentLocationSummary(internalLocationId!!, prisonCode)
      },
      inCell,
      startDate,
      startTime,
      endTime,
      comment ?: appointment.comment,
      appointment.schedule?.toRepeat(),
      false,
      isCancelled(),
      appointment.created,
      userMap[appointment.createdBy].toSummary(appointment.createdBy),
      updated,
      if (updatedBy == null) {
        null
      } else {
        userMap[updatedBy].toSummary(updatedBy!!)
      },
      prisoners.toSummary(),
    )

  private fun failIfIndividualAppointmentAlreadyAllocated() {
    if (appointment.appointmentType == AppointmentType.INDIVIDUAL && allocations.isNotEmpty()) {
      throw IllegalArgumentException("Cannot allocate multiple prisoners to an individual appointment")
    }
  }
}

fun List<AppointmentOccurrence>.toModel() = map { it.toModel() }

fun List<AppointmentOccurrence>.toSummary(
  prisonCode: String,
  locationMap: Map<Long, Location>,
  userMap: Map<String, UserDetail>,
  appointmentComment: String,
) = map { it.toSummary(prisonCode, locationMap, userMap, appointmentComment) }
