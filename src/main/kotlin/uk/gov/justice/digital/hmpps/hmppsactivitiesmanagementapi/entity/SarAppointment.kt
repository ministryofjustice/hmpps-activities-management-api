package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@Entity
@Immutable
@Table(name = "v_sar_appointment")
data class SarAppointment(
  @Id
  val appointmentId: Long,
  val prisonCode: String,
  val prisonerNumber: String,
  val categoryCode: String,
  val startDate: LocalDate,
  val startTime: LocalTime,
  val endTime: LocalTime?,
  val extraInformation: String?,
  val prisonerExtraInformation: String?,
  val attended: String,
  val createdDate: LocalDate,
  val customName: String?,
  val organiser: String?,
  val dpsLocationId: UUID?,
  val inCell: Boolean,
  val offWing: Boolean,
  val onWing: Boolean,
  val cancellationReason: String?,
  val cancelledBy: String?,

  @Transient
  var category: String,
)
