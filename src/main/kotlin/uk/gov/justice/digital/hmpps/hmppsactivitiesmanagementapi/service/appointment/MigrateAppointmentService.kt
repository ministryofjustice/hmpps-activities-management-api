package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.ifNotEmpty
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentSeries
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentCountSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo.THIS_AND_ALL_FUTURE_APPOINTMENTS
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentMigrateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentSeriesRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentSeriesSpecification
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.DELETE_MIGRATED_APPOINTMENT_CANCELLATION_REASON_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.TransactionHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.ReferenceCodeDomain
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.ReferenceCodeService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toAppointmentCategorySummary
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional
class MigrateAppointmentService(
  private val appointmentSeriesSpecification: AppointmentSeriesSpecification,
  private val appointmentSeriesRepository: AppointmentSeriesRepository,
  private val appointmentInstanceRepository: AppointmentInstanceRepository,
  private val appointmentCreateDomainService: AppointmentCreateDomainService,
  private val appointmentCancelDomainService: AppointmentCancelDomainService,
  private val appointmentRepository: AppointmentRepository,
  private val referenceCodeService: ReferenceCodeService,
  private val transactionHandler: TransactionHandler,
  @Value("\${applications.max-appointment-start-date-from-today:370}") private val maxStartDateOffsetDays: Long = 370,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  val videoBookingCategories = listOf("VLB", "VLPM")

  private fun ignoreCategoryCode(categoryCode: String) = videoBookingCategories.contains(categoryCode)

  fun migrateAppointment(request: AppointmentMigrateRequest): AppointmentInstance? {
    val appointmentDescription = with(request) {
      "$prisonCode $categoryCode $startDate $startTime-$endTime"
    }

    if (ignoreCategoryCode(request.categoryCode!!).not() && LocalDate.now().plusDays(maxStartDateOffsetDays.toLong()) < request.startDate) {
      log.warn("Appointment dropped as start date is more than $maxStartDateOffsetDays days in the future: $appointmentDescription")
      return null
    }

    val appointmentSeries =
      transactionHandler.newSpringTransaction {
        appointmentSeriesRepository.saveAndFlush(
          AppointmentSeries(
            appointmentType = AppointmentType.INDIVIDUAL,
            prisonCode = request.prisonCode!!,
            categoryCode = request.categoryCode!!,
            customName = request.comment?.trim()?.takeIf { it.isNotEmpty() && ignoreCategoryCode(request.categoryCode).not() }?.take(40),
            appointmentTier = null,
            internalLocationId = request.internalLocationId,
            startDate = request.startDate!!,
            startTime = request.startTime!!,
            endTime = request.endTime ?: run {
              with(request) {
                val newEndTime = startTime!!.plusHours(1)
                log.warn("Null end time set to start time plus one hour: $appointmentDescription")
                newEndTime
              }
            },
            extraInformation = request.comment?.trim()?.takeIf { it.isNotEmpty() },
            createdTime = request.created!!,
            createdBy = request.createdBy!!,
            updatedTime = request.updated,
            updatedBy = request.updatedBy,
            isMigrated = true,
          ),
        )
      }

    val appointmentSeriesModel = appointmentCreateDomainService.createAppointments(
      appointmentSeries = appointmentSeries,
      prisonNumberBookingIdMap = mapOf(request.prisonerNumber!! to request.bookingId!!),
      createFirstAppointmentOnly = false,
      isCancelled = request.isCancelled ?: false,
    )

    // The appointment attendee id is used for the appointment instance id as there is a one-to-one relationship between an
    // appointment attendee and appointment instances. The one way sync process is at the appointment instance level
    val appointmentInstanceId = appointmentSeriesModel.appointments.single().attendees.single().id

    return appointmentInstanceRepository.findOrThrowNotFound(appointmentInstanceId).toModel()
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

  fun getAppointmentSummary(prisonCode: String, startDate: LocalDate, categoryCodes: List<String>): List<AppointmentCountSummary> {
    val referenceCodeMap = referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY)

    val summary = mutableListOf<AppointmentCountSummary>()
    categoryCodes.forEach { categoryCode ->
      val count = appointmentRepository.countAppointmentByPrisonCodeAndCategoryCodeAndStartDateGreaterThanEqualAndIsDeleted(prisonCode, categoryCode, startDate)
      val categorySummary = referenceCodeMap[categoryCode].toAppointmentCategorySummary(categoryCode)
      summary.add(AppointmentCountSummary(count = count, appointmentCategorySummary = categorySummary))
    }
    return summary
  }
}
