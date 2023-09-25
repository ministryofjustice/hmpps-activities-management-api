package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentSeriesSpecification
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentSeriesRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.TransactionHandler
import java.time.LocalDate

@Component
class DeleteMigratedAppointmentsJob(
  private val jobRunner: SafeJobRunner,
  private val appointmentSeriesSpecification: AppointmentSeriesSpecification,
  private val appointmentSeriesRepository: AppointmentSeriesRepository,
  private val transactionHandler: TransactionHandler,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Async("asyncExecutor")
  fun execute(prisonCode: String, startDate: LocalDate, categoryCode: String? = null) {
    jobRunner.runJob(
      JobDefinition(JobType.DELETE_MIGRATED_APPOINTMENTS) {
        deleteMigratedAppointments(prisonCode, startDate, categoryCode)
      },
    )
  }

  private fun deleteMigratedAppointments(prisonCode: String, startDate: LocalDate, categoryCode: String? = null) {
    var spec = appointmentSeriesSpecification.prisonCodeEquals(prisonCode)
      .and(appointmentSeriesSpecification.startDateGreaterThanOrEquals(startDate))
      .and(appointmentSeriesSpecification.isMigrated())

    categoryCode?.apply {
      spec = spec.and(appointmentSeriesSpecification.categoryCodeEquals(categoryCode))
    }

    transactionHandler.newSpringTransaction {
      val appointmentSeriesToDelete = appointmentSeriesRepository.findAll(spec)
      val deletedAppointmentInstanceIds = appointmentSeriesToDelete.flatMap {it.appointments().flatMap { appointment -> appointment.attendees().map { attendee -> attendee.appointmentAttendeeId } } }

      appointmentSeriesToDelete.forEach {
        appointmentSeriesRepository.delete(it)
      }

      deletedAppointmentInstanceIds
    }.let {
      log.warn("Deleted ${it.size} migrated appointments for $prisonCode that started on or after $startDate" + (categoryCode?.let { " with category code $categoryCode" } ?: ""))
    }
  }
}
