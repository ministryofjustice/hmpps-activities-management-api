package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "advance_attendance")
data class AdvanceAttendance(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val advanceAttendanceId: Long = 0,

  @ManyToOne
  @JoinColumn(name = "scheduled_instance_id", nullable = false)
  val scheduledInstance: ScheduledInstance,

  val prisonerNumber: String,

  var issuePayment: Boolean,

  var recordedTime: LocalDateTime,

  var recordedBy: String,
) {

  @OneToMany(mappedBy = "advanceAttendance", cascade = [CascadeType.ALL], orphanRemoval = true)
  private var attendanceHistory: MutableList<AdvanceAttendanceHistory> = mutableListOf()

  fun history() = attendanceHistory.toList()

  @Override
  override fun toString(): String = this::class.simpleName + "(advanceAttendanceId = $advanceAttendanceId )"

  fun updatePayment(issuePayment: Boolean, recordedBy: String): AdvanceAttendance {
    updateHistory()

    this.issuePayment = issuePayment
    this.recordedBy = recordedBy
    this.recordedTime = LocalDateTime.now()
    return this
  }

  private fun updateHistory() {
    attendanceHistory.add(
      AdvanceAttendanceHistory(
        advanceAttendance = this,
        recordedTime = recordedTime,
        recordedBy = recordedBy,
        issuePayment = issuePayment,
      ),
    )
  }
}
