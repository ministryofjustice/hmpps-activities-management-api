package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.FIX_ACTIVITY_LOCATIONS
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.FIX_APPOINTMENT_SERIES_LOCATIONS
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.FIX_APPOINTMENT_SET_LOCATIONS
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.FixActivitiesLocationsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.FixAppointmentSeriesLocationsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.FixAppointmentSetLocationsService

class FixLocationsJobTest : JobsTestBase() {
  private val fixActivitiesLocationsService: FixActivitiesLocationsService = mock()
  private val fixAppointmentSeriesLocationsService: FixAppointmentSeriesLocationsService = mock()
  private val fixAppointmentSetLocationsService: FixAppointmentSetLocationsService = mock()
  private val jobDefinitionCaptor = argumentCaptor<JobDefinition>()

  private val job = FixLocationsJob(fixActivitiesLocationsService, fixAppointmentSeriesLocationsService, fixAppointmentSetLocationsService, safeJobRunner)

  @Test
  fun `fix locations job triggered`() {
    mockJobs(FIX_ACTIVITY_LOCATIONS, FIX_APPOINTMENT_SERIES_LOCATIONS, FIX_APPOINTMENT_SET_LOCATIONS)

    job.execute()

    verify(safeJobRunner, times(3)).runJob(jobDefinitionCaptor.capture())

    assertThat(jobDefinitionCaptor.allValues).extracting("jobType").containsOnly(
      FIX_ACTIVITY_LOCATIONS,
      FIX_APPOINTMENT_SERIES_LOCATIONS,
      FIX_APPOINTMENT_SET_LOCATIONS,
    )

    verify(fixActivitiesLocationsService).fixActivityLocations()
    verify(fixAppointmentSeriesLocationsService).fixLocations()
    verify(fixAppointmentSetLocationsService).fixLocations()
  }
}
