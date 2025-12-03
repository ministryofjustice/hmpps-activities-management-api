package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Column
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
import org.hibernate.envers.Audited
import org.hibernate.envers.RelationTargetAuditMode
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Audited
@Table(name = "waiting_list")
@EntityListeners(AuditableEntityListener::class)
class WaitingList(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val waitingListId: Long? = null,

  @Column(nullable = false)
  val prisonCode: String,

  @Column(nullable = false)
  val prisonerNumber: String,

  @Column(nullable = false)
  val bookingId: Long,

  @Column(nullable = false)
  var applicationDate: LocalDate,

  @ManyToOne
  @JoinColumn(name = "activity_schedule_id", nullable = false)
  @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
  val activitySchedule: ActivitySchedule,

  @Column(nullable = false)
  var requestedBy: String,

  @Column(nullable = true)
  var comments: String? = null,

  @Column(nullable = false)
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

      if (status == WaitingListStatus.WITHDRAWN) {
        require(value == WaitingListStatus.PENDING) {
          "Withdrawn waiting list can only be changed to pending"
        }
      }

      if (status == WaitingListStatus.DECLINED && value != WaitingListStatus.DECLINED) {
        declinedReason = null
      }

      if (field != value) statusUpdatedTime = LocalDateTime.now()

      field = value
    }

  @ManyToOne
  @JoinColumn(name = "activity_id", nullable = false)
  @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
  var activity: Activity = activitySchedule.activity

  @Column(nullable = false)
  val creationTime: LocalDateTime = LocalDateTime.now()

  var declinedReason: String? = null
    set(value) {
      require(status == WaitingListStatus.DECLINED) { "Cannot set the declined reason when status is not declined" }

      field = value
    }

  var updatedTime: LocalDateTime? = null

  var updatedBy: String? = null

  var statusUpdatedTime: LocalDateTime? = null

  @OneToOne
  @JoinColumn(name = "allocation_id", nullable = true)
  @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
  var allocation: Allocation? = null

  fun isStatus(vararg s: WaitingListStatus) = s.any { it == status }

  fun isPending() = isStatus(WaitingListStatus.PENDING)

  fun isApproved() = isStatus(WaitingListStatus.APPROVED)

  fun allocated(allocation: Allocation) = apply {
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
  WITHDRAWN,
}
