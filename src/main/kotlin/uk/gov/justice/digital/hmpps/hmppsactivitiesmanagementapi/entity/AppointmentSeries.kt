package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.UserDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toAppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toAppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toAppointmentName
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toSummary
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment as AppointmentModel

@Entity
@Table(name = "appointment_series")
data class AppointmentSeries(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "appointment_series_id")
  val appointmentSeriesId: Long = 0,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinTable(
    name = "appointment_set_appointment_series",
    joinColumns = [JoinColumn(name = "appointment_series_id")],
    inverseJoinColumns = [JoinColumn(name = "appointment_set_id")],
  )
  val bulkAppointment: BulkAppointment? = null,

  @Enumerated(EnumType.STRING)
  val appointmentType: AppointmentType,

  val prisonCode: String,

  val categoryCode: String,

  @Column(name = "custom_name")
  val appointmentDescription: String?,

  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "appointment_tier_id")
  var appointmentTier: AppointmentTier,

  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "appointment_host_id")
  var appointmentHost: AppointmentHost? = null,

  val internalLocationId: Long?,

  val inCell: Boolean,

  val startDate: LocalDate,

  val startTime: LocalTime,

  val endTime: LocalTime?,

  @OneToOne(fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
  @JoinColumn(name = "appointment_series_schedule_id")
  var schedule: AppointmentSchedule? = null,

  @Column(name = "extra_information")
  val comment: String?,

  @Column(name = "created_time")
  val created: LocalDateTime = LocalDateTime.now(),

  val createdBy: String,

  @Column(name = "updated_time")
  var updated: LocalDateTime? = null,

  var updatedBy: String? = null,

  val isMigrated: Boolean = false,
) {
  fun scheduleIterator() =
    schedule?.let { AppointmentScheduleIterator(startDate, schedule!!.repeatPeriod, schedule!!.repeatCount) }
      ?: AppointmentScheduleIterator(startDate, AppointmentRepeatPeriod.DAILY, 1)

  @OneToMany(mappedBy = "appointmentSeries", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @OrderBy("sequenceNumber ASC")
  private val occurrences: MutableList<AppointmentOccurrence> = mutableListOf()

  fun occurrences() = occurrences.filterNot { it.isDeleted() }.toList()

  fun scheduledOccurrences() = occurrences().filter { it.isScheduled() }.toList()

  fun scheduledOccurrencesAfter(startDateTime: LocalDateTime) = scheduledOccurrences().filter { it.startDateTime() > startDateTime }.toList()

  fun applyToOccurrences(appointmentOccurrence: AppointmentOccurrence, applyTo: ApplyTo, action: String): List<AppointmentOccurrence> {
    require(!appointmentOccurrence.isExpired()) {
      "Cannot $action a past appointment occurrence"
    }

    require(!appointmentOccurrence.isCancelled()) {
      "Cannot $action a cancelled appointment occurrence"
    }

    require(!appointmentOccurrence.isDeleted()) {
      "Cannot $action a deleted appointment occurrence"
    }

    return when (applyTo) {
      ApplyTo.THIS_AND_ALL_FUTURE_OCCURRENCES -> listOf(appointmentOccurrence).union(
        scheduledOccurrencesAfter(appointmentOccurrence.startDateTime()),
      ).toList()
      ApplyTo.ALL_FUTURE_OCCURRENCES -> scheduledOccurrences()
      else -> listOf(appointmentOccurrence)
    }
  }

  fun occurrenceDetails(
    prisonerMap: Map<String, Prisoner>,
    referenceCodeMap: Map<String, ReferenceCode>,
    locationMap: Map<Long, Location>,
    userMap: Map<String, UserDetail>,
  ) = occurrences().toDetails(prisonerMap, referenceCodeMap, locationMap, userMap)

  fun addOccurrence(occurrence: AppointmentOccurrence) = occurrences.add(occurrence)

  fun removeOccurrence(occurrence: AppointmentOccurrence) = occurrences.remove(occurrence)

  fun internalLocationIds() =
    listOf(internalLocationId).union(occurrences().map { occurrence -> occurrence.internalLocationId }).filterNotNull()

  fun prisonerNumbers(): List<String> {
    val orderedOccurrences = occurrences().sortedBy { it.startDateTime() }
    if (orderedOccurrences.isEmpty()) return emptyList()
    return (orderedOccurrences.firstOrNull { !it.isExpired() } ?: orderedOccurrences.last()).prisonerNumbers()
  }

  fun usernames() =
    listOf(createdBy, updatedBy).union(occurrences().flatMap { occurrence -> occurrence.usernames() }).filterNotNull()

  fun toModel() = AppointmentModel(
    id = appointmentSeriesId,
    appointmentType = appointmentType,
    prisonCode = prisonCode,
    categoryCode = categoryCode,
    appointmentDescription = appointmentDescription,
    internalLocationId = internalLocationId,
    inCell = inCell,
    startDate = startDate,
    startTime = startTime,
    endTime = endTime,
    repeat = schedule?.toRepeat(),
    comment = comment,
    created = created,
    createdBy = createdBy,
    updated = updated,
    updatedBy = updatedBy,
    occurrences = occurrences().toModel(),
  )

  fun toDetails(
    prisoners: List<Prisoner>,
    referenceCodeMap: Map<String, ReferenceCode>,
    locationMap: Map<Long, Location>,
    userMap: Map<String, UserDetail>,
  ) =
    AppointmentDetails(
      appointmentSeriesId,
      appointmentType,
      prisonCode,
      referenceCodeMap[categoryCode].toAppointmentName(categoryCode, appointmentDescription),
      prisoners.toSummary(),
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
      schedule?.toRepeat(),
      comment,
      created,
      userMap[createdBy].toSummary(createdBy),
      updated,
      if (updatedBy == null) {
        null
      } else {
        userMap[updatedBy].toSummary(updatedBy!!)
      },
      occurrences().toSummary(referenceCodeMap, locationMap, userMap),
    )
}

fun List<AppointmentSeries>.toModel() = map { it.toModel() }

enum class AppointmentType {
  INDIVIDUAL,
  GROUP,
}
