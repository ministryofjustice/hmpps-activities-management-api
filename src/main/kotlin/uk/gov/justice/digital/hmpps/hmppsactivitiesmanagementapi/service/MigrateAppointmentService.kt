package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentCancellationReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentSeriesRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentSeriesSpecification
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.DELETE_MIGRATED_APPOINTMENT_CANCELLATION_REASON_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional
class MigrateAppointmentService(
  private val appointmentSeriesSpecification: AppointmentSeriesSpecification,
  private val appointmentSeriesRepository: AppointmentSeriesRepository,
  private val appointmentCancellationReasonRepository: AppointmentCancellationReasonRepository,
  private val transactionHandler: TransactionHandler,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun deleteMigratedAppointments(prisonCode: String, startDate: LocalDate, categoryCode: String? = null) {
    log.warn("Soft deleting migrated appointments for '$prisonCode' that started on or after $startDate" + (categoryCode?.let { " with category code '$categoryCode'" } ?: ""))

    val now = LocalDateTime.now()
    val cancellationReason = appointmentCancellationReasonRepository.findOrThrowNotFound(DELETE_MIGRATED_APPOINTMENT_CANCELLATION_REASON_ID)
    val user = "DELETE_MIGRATED_APPOINTMENT_SERVICE"

    var spec = appointmentSeriesSpecification.prisonCodeEquals(prisonCode)
      .and(appointmentSeriesSpecification.startDateGreaterThanOrEquals(startDate))
      .and(appointmentSeriesSpecification.isMigrated())

    categoryCode?.apply {
      spec = spec.and(appointmentSeriesSpecification.categoryCodeEquals(categoryCode))
    }

    appointmentSeriesRepository.findAll(spec).let {
      it.forEach { appointmentSeries ->
        transactionHandler.newSpringTransaction {
          appointmentSeries.appointments().forEach { appointment ->
            appointment.cancel(now, cancellationReason, user)
            log.info("Soft deleted migrated appointment with id '${appointment.appointmentId}'")
          }
        }
      }
      log.warn("Soft deleted ${it.size} migrated appointments for '$prisonCode' that started on or after $startDate" + (categoryCode?.let { " with category code '$categoryCode'" } ?: ""))
    }
  }
}
