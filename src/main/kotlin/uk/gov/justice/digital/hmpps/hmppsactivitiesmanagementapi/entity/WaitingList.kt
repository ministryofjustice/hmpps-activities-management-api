package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "waiting_list")
class WaitingList(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val waitingListId: Long = 0,

  val prisonCode: String,

  val prisonerNumber: String,

  val bookingId: Long,

  val applicationDate: LocalDate,

  @OneToOne
  @JoinColumn(name = "activity_schedule_id", nullable = false)
  val activitySchedule: ActivitySchedule,

  val requestedBy: String,

  @Enumerated(EnumType.STRING)
  var status: WaitingListStatus,

  val createdBy: String,
) {
  val creationTime: LocalDateTime = LocalDateTime.now()

  var declinedReason: String? = null

  var updatedTime: LocalDateTime? = null

  var updatedBy: String? = null

  @OneToOne
  @JoinColumn(name = "allocation_id", nullable = true)
  var allocation: Allocation? = null
}

enum class WaitingListStatus {
  PENDING,
  APPROVED,
  DECLINED,
  ALLOCATED,
  REMOVED,
}
