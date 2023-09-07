package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Where
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.UserDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentOccurrenceDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentOccurrenceSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toAppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toAppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toAppointmentName
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toSummary
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentOccurrence as AppointmentOccurrenceModel

@Entity
@Table(name = "appointment")
@Where(clause = "NOT is_deleted")
@EntityListeners(AppointmentOccurrenceEntityListener::class)
data class AppointmentOccurrence(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "appointment_id")
  val appointmentOccurrenceId: Long = 0,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "appointment_series_id", nullable = false)
  val appointmentSeries: AppointmentSeries,

  val sequenceNumber: Int,

  val prisonCode: String,

  var categoryCode: String,

  @Column(name = "custom_name")
  var appointmentDescription: String?,

  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "appointment_tier_id")
  var appointmentTier: AppointmentTier,

  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "appointment_host_id")
  var appointmentHost: AppointmentHost? = null,

  var internalLocationId: Long?,

  var inCell: Boolean,

  var startDate: LocalDate,

  var startTime: LocalTime,

  var endTime: LocalTime?,

  @Column(name = "extra_information")
  var comment: String?,

  @Column(name = "created_time")
  val created: LocalDateTime,

  val createdBy: String,

  @Column(name = "updated_time")
  var updated: LocalDateTime? = null,

  var updatedBy: String? = null,
) {
  @Column(name = "cancelled_time")
  var cancelled: LocalDateTime? = null

  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "cancellation_reason_id")
  var cancellationReason: AppointmentCancellationReason? = null

  var cancelledBy: String? = null

  @Column(name = "is_deleted")
  var deleted: Boolean = false

  @OneToMany(mappedBy = "appointmentOccurrence", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  private val allocations: MutableList<AppointmentOccurrenceAllocation> = mutableListOf()

  fun allocations() = allocations.toList()

  fun addAllocation(allocation: AppointmentOccurrenceAllocation) {
    failIfIndividualAppointmentAlreadyAllocated()
    allocations.add(allocation)
  }

  fun removeAllocation(allocation: AppointmentOccurrenceAllocation) = allocations.remove(allocation)

  fun prisonerNumbers() = allocations().map { allocation -> allocation.prisonerNumber }.distinct()

  fun startDateTime(): LocalDateTime = LocalDateTime.of(startDate, startTime)

  fun isScheduled() = !isExpired() && !isCancelled() && !isDeleted()

  fun isEdited() = updated != null

  fun isCancelled() = cancellationReason?.isDelete == false

  fun isExpired() = startDateTime() < LocalDateTime.now()

  fun isDeleted() = cancellationReason?.isDelete == true

  fun usernames() = listOf(createdBy, updatedBy, cancelledBy).filterNotNull()

  fun toModel() = AppointmentOccurrenceModel(
    id = appointmentOccurrenceId,
    sequenceNumber = sequenceNumber,
    categoryCode = categoryCode,
    appointmentDescription = appointmentDescription,
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
    allocations = allocations().toModel(),
  )

  fun toSummary(
    referenceCodeMap: Map<String, ReferenceCode>,
    locationMap: Map<Long, Location>,
    userMap: Map<String, UserDetail>,
  ) =
    AppointmentOccurrenceSummary(
      appointmentOccurrenceId,
      sequenceNumber,
      referenceCodeMap[categoryCode].toAppointmentName(categoryCode, appointmentDescription),
      referenceCodeMap[categoryCode].toAppointmentCategorySummary(categoryCode),
      appointmentDescription,
      if (inCell) {
        null
      } else {
        locationMap[internalLocationId].toAppointmentLocationSummary(
          internalLocationId!!,
          prisonCode,
        )
      },
      inCell,
      startDate,
      startTime,
      endTime,
      comment,
      isEdited = isEdited(),
      isCancelled = isCancelled(),
      updated = updated,
      updatedBy?.let { userMap[updatedBy].toSummary(updatedBy!!) },
    )

  fun toDetails(
    prisonerMap: Map<String, Prisoner>,
    referenceCodeMap: Map<String, ReferenceCode>,
    locationMap: Map<Long, Location>,
    userMap: Map<String, UserDetail>,
  ) =
    AppointmentOccurrenceDetails(
      appointmentOccurrenceId,
      appointmentSeries.appointmentSeriesId,
      appointmentSeries.bulkAppointment?.toSummary(),
      appointmentSeries.appointmentType,
      sequenceNumber,
      prisonCode,
      referenceCodeMap[categoryCode].toAppointmentName(categoryCode, appointmentDescription),
      allocations().map { prisonerMap[it.prisonerNumber].toSummary(prisonCode, it.prisonerNumber, it.bookingId) },
      referenceCodeMap[categoryCode].toAppointmentCategorySummary(categoryCode),
      appointmentDescription,
      if (inCell) {
        null
      } else {
        locationMap[internalLocationId].toAppointmentLocationSummary(internalLocationId!!, prisonCode)
      },
      inCell,
      startDate,
      startTime,
      endTime,
      comment,
      appointmentSeries.schedule?.toRepeat(),
      isEdited(),
      isCancelled(),
      isExpired(),
      appointmentSeries.created,
      userMap[appointmentSeries.createdBy].toSummary(appointmentSeries.createdBy),
      updated,
      if (updatedBy == null) {
        null
      } else {
        userMap[updatedBy].toSummary(updatedBy!!)
      },
      cancelled,
      if (cancelledBy == null) {
        null
      } else {
        userMap[cancelledBy].toSummary(cancelledBy!!)
      },
    )

  private fun failIfIndividualAppointmentAlreadyAllocated() {
    if (appointmentSeries.appointmentType == AppointmentType.INDIVIDUAL && allocations().isNotEmpty()) {
      throw IllegalArgumentException("Cannot allocate multiple prisoners to an individual appointment")
    }
  }
}

fun List<AppointmentOccurrence>.toModel() = map { it.toModel() }

fun List<AppointmentOccurrence>.toSummary(
  referenceCodeMap: Map<String, ReferenceCode>,
  locationMap: Map<Long, Location>,
  userMap: Map<String, UserDetail>,
) = map { it.toSummary(referenceCodeMap, locationMap, userMap) }

fun List<AppointmentOccurrence>.toDetails(
  prisonerMap: Map<String, Prisoner>,
  referenceCodeMap: Map<String, ReferenceCode>,
  locationMap: Map<Long, Location>,
  userMap: Map<String, UserDetail>,
) = map { it.toDetails(prisonerMap, referenceCodeMap, locationMap, userMap) }
