package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.ifNotEmpty
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentCreateDomainService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentCancelDomainService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSeries
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo.THIS_AND_ALL_FUTURE_APPOINTMENTS
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentMigrateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentSeriesRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentSeriesSpecification
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentTierRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentCancellationReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.NOT_SPECIFIED_APPOINTMENT_TIER_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.DELETE_MIGRATED_APPOINTMENT_CANCELLATION_REASON_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import java.security.Principal
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional
class MigrateAppointmentService(
  private val appointmentSeriesSpecification: AppointmentSeriesSpecification,
  private val appointmentSeriesRepository: AppointmentSeriesRepository,
  private val appointmentTierRepository: AppointmentTierRepository,
  private val appointmentCancellationReasonRepository: AppointmentCancellationReasonRepository,
  private val appointmentCreateDomainService: AppointmentCreateDomainService,
  private val appointmentCancelDomainService: AppointmentCancelDomainService,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun migrateAppointment(request: AppointmentMigrateRequest): AppointmentInstance {
    val appointmentTier = appointmentTierRepository.findOrThrowNotFound(NOT_SPECIFIED_APPOINTMENT_TIER_ID)

    val appointmentSeries = appointmentSeriesRepository.saveAndFlush(
      AppointmentSeries(
        appointmentType = AppointmentType.INDIVIDUAL,
        prisonCode = request.prisonCode!!,
        categoryCode = request.categoryCode!!,
        customName = request.comment?.trim()?.take(40),
        appointmentTier = appointmentTier,
        internalLocationId = request.internalLocationId,
        startDate = request.startDate!!,
        startTime = request.startTime!!,
        endTime = request.endTime,
        extraInformation = request.comment?.trim()?.takeIf { it.length > 40 },
        createdTime = request.created!!,
        createdBy = request.createdBy!!,
        updatedTime = request.updated,
        updatedBy = request.updatedBy,
        isMigrated = true,
      )
    )

    val =  appointmentCreateDomainService.createAppointments(
      appointmentSeries,
      mapOf(request.prisonerNumber!! to request.bookingId!!),
      isCancelled = request.isCancelled ?: false
    )

    val prisonerBookings = mapOf(request.prisonerNumber!! to request.bookingId.toString())
    val appointmentSeries = appointmentSeriesRepository.saveAndFlush(
      buildValidAppointmentSeriesEntity(
        appointmentType = AppointmentType.INDIVIDUAL,
        prisonCode = request.prisonCode!!,
        prisonerNumbers = listOf(request.prisonerNumber),
        prisonerBookings = prisonerBookings,
        categoryCode = request.categoryCode!!,
        customName = request.comment?.trim()?.take(40),
        appointmentTier = appointmentTier,
        internalLocationId = request.internalLocationId,
        inCell = false,
        startDate = request.startDate,
        startTime = request.startTime,
        endTime = request.endTime,
        extraInformation = request.comment?.trim()?.takeIf { it.length > 40 },
        createdTime = request.created!!,
        createdBy = request.createdBy!!,
        updatedTime = request.updated,
        updatedBy = request.updatedBy,
        isCancelled = request.isCancelled ?: false,
        isMigrated = true,
      ),
    )
    val appointmentInstance = appointmentInstanceRepository.findOrThrowNotFound(appointmentSeries.appointments().first().attendees().first().appointmentAttendeeId)
    return appointmentInstance.toModel()
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
