package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import org.hibernate.annotations.SQLDelete
import org.hibernate.annotations.Where
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.UserDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toAppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toAppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toSummary
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment as AppointmentModel

@Entity
@Table(name = "appointment")
@SQLDelete(sql = "UPDATE appointment SET deleted = true WHERE appointment_id = ?")
@Where(clause = "deleted = false")
data class Appointment(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val appointmentId: Long = 0,

  var categoryCode: String,

  val prisonCode: String,

  var internalLocationId: Long?,

  var inCell: Boolean,

  var startDate: LocalDate,

  var startTime: LocalTime,

  var endTime: LocalTime?,

  @OneToOne(fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
  @JoinColumn(name = "appointment_schedule_id")
  var schedule: AppointmentSchedule? = null,

  var comment: String,

  val created: LocalDateTime = LocalDateTime.now(),

  val createdBy: String,

  var updated: LocalDateTime? = null,

  var updatedBy: String? = null,

  val deleted: Boolean = false,
) {
  fun scheduleIterator() = schedule?.let { AppointmentScheduleIterator(startDate, schedule!!.repeatPeriod, schedule!!.repeatCount) } ?: AppointmentScheduleIterator(startDate, AppointmentRepeatPeriod.DAILY, 1)

  @OneToMany(mappedBy = "appointment", fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
  @Fetch(FetchMode.SUBSELECT)
  @OrderBy("sequenceNumber ASC")
  private val occurrences: MutableList<AppointmentOccurrence> = mutableListOf()

  fun occurrences() = occurrences.toList()

  fun addOccurrence(occurrence: AppointmentOccurrence) = occurrences.add(occurrence)

  fun internalLocationIds() = listOf(internalLocationId).union(occurrences().map { occurrence -> occurrence.internalLocationId }).filterNotNull()

  fun prisonerNumbers() = occurrences().map { occurrence -> occurrence.prisonerNumbers() }.flatten().distinct()

  fun usernames() = listOf(createdBy, updatedBy).union(occurrences().map { occurrence -> occurrence.updatedBy }).filterNotNull()

  fun toModel() = AppointmentModel(
    id = appointmentId,
    categoryCode = categoryCode,
    prisonCode = prisonCode,
    internalLocationId = internalLocationId,
    inCell = inCell,
    startDate = startDate,
    startTime = startTime,
    endTime = endTime,
    comment = comment,
    created = created,
    createdBy = createdBy,
    updated = updated,
    updatedBy = updatedBy,
    occurrences = occurrences.toModel(),
  )

  fun toDetails(referenceCodeMap: Map<String, ReferenceCode>, locationMap: Map<Long, Location>, userMap: Map<String, UserDetail>, prisoners: List<Prisoner>) =
    AppointmentDetails(
      appointmentId,
      referenceCodeMap[categoryCode].toAppointmentCategorySummary(categoryCode),
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
      occurrences().toSummary(prisonCode, locationMap, userMap, comment),
      prisoners.toSummary(),
    )
}

fun List<Appointment>.toModel() = map { it.toModel() }
