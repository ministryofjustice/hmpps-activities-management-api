package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.pentonvillePrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AttendanceUpdateRequest

class AttendanceUpdateRequestValidatorTest {

  private val waiting =
    AttendanceUpdateRequest(id = 1, prisonCode = moorlandPrisonCode, status = AttendanceStatus.WAITING)
  private val completed =
    AttendanceUpdateRequest(id = 2, prisonCode = pentonvillePrisonCode, status = AttendanceStatus.COMPLETED)

  @Test
  fun `check attributes are null when waiting`() {
    val message =
      "Reason, comment, issue payment, case note, incentive level warning issued and other absence reason must be null if status is waiting for attendance ID 1"

    shouldError(waiting.copy(attendanceReason = "ATTENDED"), message)
    shouldError(waiting.copy(comment = "Some comments"), message)
    shouldError(waiting.copy(issuePayment = true), message)
    shouldError(waiting.copy(caseNote = "case note"), message)
    shouldError(waiting.copy(incentiveLevelWarningIssued = true), message)
    shouldError(waiting.copy(otherAbsenceReason = "other absence reason"), message)

    shouldNotError(waiting)
  }

  @Test
  fun `check attendance reason is provided when completed`() {
    shouldError(
      completed.copy(attendanceReason = null),
      "Attendance reason must be supplied when status is completed for attendance ID 2",
    )
  }

  @Test
  fun `check attributes when attendance reason is ATTENDED`() {
    shouldError(
      completed.copy(attendanceReason = "ATTENDED"),
      "Issue payment is required when reason is attended for attendance ID 2",
    )
    shouldError(
      completed.copy(attendanceReason = "ATTENDED", issuePayment = false),
      "Case note is required when issue payment is not required when reason is attended for attendance ID 2",
    )

    shouldNotError(completed.copy(attendanceReason = "ATTENDED", issuePayment = true))
    shouldNotError(completed.copy(attendanceReason = "ATTENDED", issuePayment = false, caseNote = "some case notes"))
  }

  @Test
  fun `check attributes when attendance reason is NOT REQUIRED (or EXCUSED)`() {
    shouldError(
      completed.copy(attendanceReason = "NOT_REQUIRED"),
      "Issue payment is required when reason is not required for attendance ID 2",
    )
    shouldError(
      completed.copy(attendanceReason = "NOT_REQUIRED", issuePayment = false),
      "Issue payment is required when reason is not required for attendance ID 2",
    )
    val message = "Case note, comments and other absence reason is not required when reason is attended for attendance ID 2"
    shouldError(completed.copy(attendanceReason = "NOT_REQUIRED", issuePayment = true, caseNote = "some case notes"), message)
    shouldError(completed.copy(attendanceReason = "NOT_REQUIRED", issuePayment = true, comment = "some comments"), message)
    shouldError(completed.copy(attendanceReason = "NOT_REQUIRED", issuePayment = true, incentiveLevelWarningIssued = false), message)
    shouldError(completed.copy(attendanceReason = "NOT_REQUIRED", issuePayment = true, otherAbsenceReason = "other absence reason"), message)

    shouldNotError(completed.copy(attendanceReason = "NOT_REQUIRED", issuePayment = true))
  }

  @Test
  fun `check attributes when attendance reason is REFUSED`() {
    val message =
      "Comment, other absence reason and issue payment must be null if reason is refused for attendance ID 2"

    shouldError(completed.copy(attendanceReason = "REFUSED", comment = "some comments"), message)
    shouldError(completed.copy(attendanceReason = "REFUSED", issuePayment = false), message)
    shouldError(completed.copy(attendanceReason = "REFUSED", otherAbsenceReason = "other absence reason"), message)
    shouldError(
      completed.copy(attendanceReason = "REFUSED", caseNote = "some case notes"),
      "Case note and incentive level warning must be supplied if reason is refused for attendance ID 2",
    )
    shouldError(
      completed.copy(attendanceReason = "REFUSED", incentiveLevelWarningIssued = true),
      "Case note and incentive level warning must be supplied if reason is refused for attendance ID 2",
    )

    shouldNotError(
      completed.copy(
        attendanceReason = "REFUSED",
        caseNote = "some case notes",
        incentiveLevelWarningIssued = true,
      ),
    )
    shouldNotError(
      completed.copy(
        attendanceReason = "REFUSED",
        caseNote = "some case notes",
        incentiveLevelWarningIssued = false,
      ),
    )
  }

  @Test
  fun `check attributes when attendance reason is SICK`() {
    val message =
      "Case note, incentive level warning issued and other absence reason must be null if reason is sick for attendance ID 2"

    shouldError(completed.copy(attendanceReason = "SICK", caseNote = "case note"), message)
    shouldError(completed.copy(attendanceReason = "SICK", incentiveLevelWarningIssued = true), message)
    shouldError(completed.copy(attendanceReason = "SICK", otherAbsenceReason = "other absence reason"), message)
    shouldError(
      completed.copy(attendanceReason = "SICK"),
      "Issue payment must be supplied if reason is sick for attendance ID 2",
    )

    shouldNotError(completed.copy(attendanceReason = "SICK", issuePayment = true))
    shouldNotError(completed.copy(attendanceReason = "SICK", issuePayment = false))
  }

  private fun shouldError(request: AttendanceUpdateRequest, errorMessage: String) {
    assertThatThrownBy {
      AttendanceUpdateRequestValidator.validate(request)
    }.isInstanceOf(ValidationException::class.java)
      .hasMessage(errorMessage)
  }

  private fun shouldNotError(request: AttendanceUpdateRequest) {
    assertDoesNotThrow { AttendanceUpdateRequestValidator.validate(request) }
  }
}
