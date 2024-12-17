package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentAttendanceMarkedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.toModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventTierType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentAttendanceSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentAttendanceRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.MultipleAppointmentAttendanceRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AppointmentAttendeeByStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentAttendanceSummaryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentAttendeeSearchRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.LocationService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.TransactionHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.ReferenceCodeDomain
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.ReferenceCodeService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.toTelemetryMetricsMap
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.toTelemetryPropertiesMap
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.checkCaseloadAccess
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toAppointmentName
import java.security.Principal
import java.time.LocalDate
import java.time.LocalDateTime

private enum class QueryMode {
  ALL,
  BY_CATEGORY_CODE,
  BY_CUSTOM_NAME,
  BY_CATEGORY_CODE_AND_CUSTOM_NAME,
}

enum class AttendanceStatus {
  ATTENDED,
  NOT_ATTENDED,
  CANCELLED,
  NOT_RECORDED,
  EVENT_TIER,
}

@Service
@Transactional
class AppointmentAttendanceService(
  private val appointmentAttendanceSummaryRepository: AppointmentAttendanceSummaryRepository,
  private val appointmentRepository: AppointmentRepository,
  private val referenceCodeService: ReferenceCodeService,
  private val locationService: LocationService,
  private val appointmentAttendeeSearchRepository: AppointmentAttendeeSearchRepository,
  private val telemetryClient: TelemetryClient,
  private val transactionHandler: TransactionHandler,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getAppointmentAttendanceSummaries(
    prisonCode: String,
    date: LocalDate,
    categoryCode: String? = null,
    customName: String? = null,
  ): List<AppointmentAttendanceSummary> {
    checkCaseloadAccess(prisonCode)

    val summaries = when (
      getQueryMode(
        categoryCode = categoryCode,
        customName = customName,
      )
    ) {
      QueryMode.ALL -> appointmentAttendanceSummaryRepository.findByPrisonCodeAndStartDate(prisonCode, date)
      QueryMode.BY_CATEGORY_CODE -> appointmentAttendanceSummaryRepository.findByPrisonCodeAndStartDateAndCategoryCode(prisonCode, date, categoryCode!!)
      QueryMode.BY_CUSTOM_NAME -> appointmentAttendanceSummaryRepository.findByPrisonCodeAndStartDateAndCustomNameIgnoreCase(prisonCode, date, customName!!)
      QueryMode.BY_CATEGORY_CODE_AND_CUSTOM_NAME -> appointmentAttendanceSummaryRepository.findByPrisonCodeAndStartDateAndCategoryCodeAndCustomNameIgnoreCase(prisonCode, date, categoryCode!!, customName!!)
    }
    val referenceCodeMap = referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY)
    val locationMap = locationService.getLocationsForAppointmentsMap(prisonCode)
    val attendeeMap = appointmentAttendeeSearchRepository.findByAppointmentIds(summaries.map { it.appointmentId })
      .groupBy { it.appointmentSearch.appointmentId }

    return summaries.toModel(attendeeMap, referenceCodeMap, locationMap)
  }

  fun getAppointmentAttendanceByStatus(
    prisonCode: String,
    status: AttendanceStatus,
    date: LocalDate,
    categoryCode: String? = null,
    customName: String? = null,
    prisonerNumber: String? = null,
    eventTier: EventTierType? = null,
    organiserCode: String? = null,
  ): List<AppointmentAttendeeByStatus> {
    if (status == AttendanceStatus.EVENT_TIER) eventTier ?: throw ValidationException("event tier filter is required")

    val referenceCodeMap = referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY)
    val appointmentAttendees = appointmentRepository.getAppointmentsWithAttendees(
      prisonCode = prisonCode,
      date = date,
      categoryCode = categoryCode,
      customName = customName,
      prisonerNumber = prisonerNumber,
      isCancelled = status == AttendanceStatus.CANCELLED,
      organiserCode = organiserCode,
    ).filter {
      when (status) {
        AttendanceStatus.ATTENDED -> it.getAttended() == true
        AttendanceStatus.NOT_ATTENDED -> it.getAttended() == false
        AttendanceStatus.NOT_RECORDED -> it.getAttended() == null
        AttendanceStatus.EVENT_TIER -> it.getEventTier() == eventTier?.name && it.getAttended() == true
        else -> true
      }
    }

    return appointmentAttendees.map {
      AppointmentAttendeeByStatus(
        prisonerNumber = it.getPrisonerNumber(),
        bookingId = it.getBookingId(),
        appointmentId = it.getAppointmentId(),
        appointmentName = referenceCodeMap[it.getCategoryCode()].toAppointmentName(it.getCategoryCode(), it.getCustomName()),
        appointmentAttendeeId = it.getAppointmentAttendeeId(),
        startDate = it.getStartDate(),
        startTime = it.getStartTime(),
        endTime = it.getEndTime(),
      )
    }
  }

  fun markAttendance(appointmentId: Long, request: AppointmentAttendanceRequest, principal: Principal): Appointment {
    val attendanceRecordedTime = LocalDateTime.now()
    val attendanceRecordedBy = principal.name

    val appointment = appointmentRepository.findOrThrowNotFound(appointmentId)
    checkCaseloadAccess(appointment.prisonCode)

    appointment.markPrisonerAttendance(request.attendedPrisonNumbers, request.nonAttendedPrisonNumbers, attendanceRecordedTime, attendanceRecordedBy)

    return appointmentRepository.saveAndFlush(appointment).toModel()
  }

  fun markMultipleAttendances(requests: List<MultipleAppointmentAttendanceRequest>, action: AttendanceAction, principal: Principal) {
    log.info("Marking multiple appointment attendances")

    val attendanceRecordedTime = LocalDateTime.now()
    val attendanceRecordedBy = principal.name

    val events = mutableListOf<AppointmentAttendanceMarkedEvent>()

    val appointments = appointmentRepository.findByIds(requests.map { it.appointmentId!! })

    appointments.forEach { appointments -> checkCaseloadAccess(appointments.prisonCode) }

    val appointmentsMap = appointments.associateBy { it.appointmentId }

    transactionHandler.newSpringTransaction {
      requests.filter { appointmentsMap.containsKey(it.appointmentId) }
        .forEach { request ->

          val appointment = appointmentsMap[request.appointmentId]!!

          val event = AppointmentAttendanceMarkedEvent(
            appointmentId = appointment.appointmentId,
            prisonCode = appointment.prisonCode,
            attendanceRecordedTime = attendanceRecordedTime,
            attendanceRecordedBy = attendanceRecordedBy,
          )

          appointment.findAttendees(request.prisonerNumbers).forEach { attendance ->
            val oldAttendedState = attendance.attended

            val newAttendedState = when (action) {
              AttendanceAction.ATTENDED -> {
                attendance.attendanceRecordedTime = attendanceRecordedTime
                attendance.attendanceRecordedBy = attendanceRecordedBy
                event.attendedPrisonNumbers.add(attendance.prisonerNumber)
                true
              }
              AttendanceAction.NOT_ATTENDED -> {
                attendance.attendanceRecordedTime = attendanceRecordedTime
                attendance.attendanceRecordedBy = attendanceRecordedBy
                event.nonAttendedPrisonNumbers.add(attendance.prisonerNumber)
                false
              }
              AttendanceAction.RESET -> {
                attendance.attendanceRecordedTime = null
                attendance.attendanceRecordedBy = null
                null
              }
            }

            attendance.attended = newAttendedState

            if (oldAttendedState != null && oldAttendedState != newAttendedState) {
              event.attendanceChangedPrisonNumbers.add(attendance.prisonerNumber)
            }
          }

          appointmentRepository.saveAndFlush(appointment)

          events.add(event)
        }
    }

    events.forEach { event ->
      val telemetryPropertiesMap = event.toTelemetryPropertiesMap()
      val telemetryMetricsMap = event.toTelemetryMetricsMap()
      telemetryClient.trackEvent(
        TelemetryEvent.APPOINTMENT_ATTENDANCE_MARKED_METRICS.value,
        telemetryPropertiesMap,
        telemetryMetricsMap,
      )
    }
  }

  private fun getQueryMode(categoryCode: String?, customName: String?): QueryMode {
    if (categoryCode == null && customName == null) return QueryMode.ALL
    if (categoryCode != null && customName != null) return QueryMode.BY_CATEGORY_CODE_AND_CUSTOM_NAME
    if (categoryCode != null) return QueryMode.BY_CATEGORY_CODE
    return QueryMode.BY_CUSTOM_NAME
  }
}

enum class AttendanceAction {
  ATTENDED,
  NOT_ATTENDED,
  RESET,
}
