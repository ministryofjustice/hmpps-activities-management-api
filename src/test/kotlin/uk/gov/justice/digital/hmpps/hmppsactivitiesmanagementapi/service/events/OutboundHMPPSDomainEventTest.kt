package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OutboundHMPPSDomainEventTest {
  @Test
  fun `validate ids`() {
    assertThat(ScheduleCreatedInformation(222).primaryId()).isEqualTo("222")
    assertThat(ScheduleCreatedInformation(222).secondaryId()).isNull()

    assertThat(ScheduledInstanceInformation(222).primaryId()).isEqualTo("222")
    assertThat(ScheduledInstanceInformation(222).secondaryId()).isNull()

    assertThat(PrisonerAllocatedInformation(222).primaryId()).isEqualTo("222")
    assertThat(PrisonerAllocatedInformation(222).secondaryId()).isNull()

    assertThat(PrisonerAttendanceInformation(222).primaryId()).isEqualTo("222")
    assertThat(PrisonerAttendanceInformation(222).secondaryId()).isNull()

    assertThat(PrisonerAttendanceDeleteInformation(222, 333).primaryId()).isEqualTo("222")
    assertThat(PrisonerAttendanceDeleteInformation(222, 333).secondaryId()).isEqualTo("333")

    assertThat(AppointmentInstanceInformation(222, "TEST").primaryId()).isEqualTo("222")
    assertThat(AppointmentInstanceInformation(222, "TEST").secondaryId()).isEqualTo("TEST")
  }
}
