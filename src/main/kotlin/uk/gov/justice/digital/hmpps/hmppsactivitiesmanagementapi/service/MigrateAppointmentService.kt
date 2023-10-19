package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.ifNotEmpty
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentCancelDomainService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo.THIS_AND_ALL_FUTURE_APPOINTMENTS
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentSeriesRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentSeriesSpecification
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.DELETE_MIGRATED_APPOINTMENT_CANCELLATION_REASON_ID
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional
class MigrateAppointmentService(
  private val appointmentSeriesSpecification: AppointmentSeriesSpecification,
  private val appointmentSeriesRepository: AppointmentSeriesRepository,
  private val appointmentCancelDomainService: AppointmentCancelDomainService,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun deleteMigratedAppointments(prisonCode: String, startDate: LocalDate, categoryCode: String? = null) {
    log.warn("Soft deleting migrated appointments for '$prisonCode' that started on or after $startDate{}", (categoryCode?.let { " with category code '$categoryCode'" } ?: ""))

    val cancelledTime = LocalDateTime.now()
    val cancelledBy = "DELETE_MIGRATED_APPOINTMENT_SERVICE"

    var spec = appointmentSeriesSpecification.prisonCodeEquals(prisonCode)
      .and(appointmentSeriesSpecification.startDateGreaterThanOrEquals(startDate))
      .and(appointmentSeriesSpecification.isMigrated())

    categoryCode?.apply {
      spec = spec.and(appointmentSeriesSpecification.categoryCodeEquals(categoryCode))
    }

    appointmentSeriesRepository.findAll(spec).let {
      var count = 0
      it.forEach { appointmentSeries ->
        val startTimeInMs = System.currentTimeMillis()
        appointmentSeries.appointments().filter { appointment -> appointment.startDate >= startDate }
          .ifNotEmpty { appointments ->
            appointmentCancelDomainService.cancelAppointments(
              appointmentSeries,
              appointments.first().appointmentId,
              appointments.toSet(),
              AppointmentCancelRequest(DELETE_MIGRATED_APPOINTMENT_CANCELLATION_REASON_ID, THIS_AND_ALL_FUTURE_APPOINTMENTS),
              cancelledTime,
              cancelledBy,
              appointments.size,
              appointments.flatMap { appointment -> appointment.attendees() }.size,
              startTimeInMs,
              trackEvent = false,
              auditEvent = true,
            )

            count += appointments.size

            log.info("Soft deleted migrated appointment(s) with id(s) '${appointments.map { appointment -> appointment.appointmentId }.joinToString("', '")}' from series with id '${appointmentSeries.appointmentSeriesId}'")
          }
      }
      log.warn("Soft deleted $count migrated appointments for '$prisonCode' that started on or after $startDate{}", (categoryCode?.let { " with category code '$categoryCode'" } ?: ""))
    }
  }
}
