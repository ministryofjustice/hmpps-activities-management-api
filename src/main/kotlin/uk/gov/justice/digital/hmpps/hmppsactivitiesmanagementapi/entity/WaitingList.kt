package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "waiting_list")
@EntityListeners(AuditableEntityListener::class)
data class WaitingList(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val waitingListId: Long = 0,

  val prisonCode: String,

  val prisonerNumber: String,

  val bookingId: Long,

  var applicationDate: LocalDate,

  @ManyToOne
  @JoinColumn(name = "activity_schedule_id", nullable = false)
  val activitySchedule: ActivitySchedule,

  var requestedBy: String,

  var comments: String? = null,

  val createdBy: String,

  @Transient
  private val initialStatus: WaitingListStatus,
) {
  @Enumerated(EnumType.STRING)
  var status: WaitingListStatus = initialStatus
    set(value) {
      if (value == WaitingListStatus.DECLINED) {
        require(isStatus(WaitingListStatus.PENDING, WaitingListStatus.APPROVED)) {
          "Only pending and approved waiting lists can be declined"
        }
      }

      if (status == WaitingListStatus.DECLINED && value != WaitingListStatus.DECLINED) {
        declinedReason = null
      }

      field = value
    }

  @ManyToOne
  @JoinColumn(name = "activity_id", nullable = false)
  val activity: Activity = activitySchedule.activity

  val creationTime: LocalDateTime = LocalDateTime.now()

  var declinedReason: String? = null
    set(value) {
      require(status == WaitingListStatus.DECLINED) { "Cannot set the declined reason when status is not declined" }

      field = value
    }

  var updatedTime: LocalDateTime? = null

  var updatedBy: String? = null

  @OneToOne
  @JoinColumn(name = "allocation_id", nullable = true)
  var allocation: Allocation? = null

  fun isStatus(vararg s: WaitingListStatus) = s.any { it == status }

  fun allocated(allocation: Allocation) =
    apply {
      require(allocation.prisonCode() == prisonCode) {
        "Allocation ${allocation.allocationId} prison does not match with waiting list $waitingListId"
      }

      require(allocation.prisonerNumber == prisonerNumber) {
        "Allocation ${allocation.allocationId} prisoner number does not match with waiting list $waitingListId"
      }

      require(status != WaitingListStatus.ALLOCATED) {
        "Waiting list $waitingListId is already allocated"
      }

      this.status = WaitingListStatus.ALLOCATED
      this.allocation = allocation
    }
}

enum class WaitingListStatus {
  PENDING,
  APPROVED,
  DECLINED,
  ALLOCATED,
  REMOVED,
}
