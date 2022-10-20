package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ScheduledInstanceRepository
import javax.persistence.EntityNotFoundException
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Attendance as ModelAttendance

@Service
class AttendancesService(private val scheduledInstanceRepository: ScheduledInstanceRepository) {

  fun findAttendancesByScheduledInstance(instanceId: Long): List<ModelAttendance> =
    scheduledInstanceRepository.findById(instanceId).orElseThrow {
      EntityNotFoundException(
        "$instanceId"
      )
    }.attendances.map { transform(it) }
}
