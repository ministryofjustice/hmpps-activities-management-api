package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.toModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.attendanceReasonEntities
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.AttendanceReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.AttendanceReasonService

class AttendanceReasonServiceTest {
  private val attendanceReasonRepository: AttendanceReasonRepository = mock()

  private val service = AttendanceReasonService(attendanceReasonRepository)

  @Test
  fun `findAll returns all appointment categories`() {
    val entities = attendanceReasonEntities()
    whenever(attendanceReasonRepository.findAll()).thenReturn(entities)
    assertThat(service.getAll()).isEqualTo(entities.toModel())
  }
}
