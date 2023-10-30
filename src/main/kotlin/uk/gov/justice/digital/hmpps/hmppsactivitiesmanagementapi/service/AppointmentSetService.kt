package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentAttendee
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSeries
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSet
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.createAppointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSetDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentSetCreatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentSetAppointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentSetCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentHostRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentSetRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentTierRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.NOT_SPECIFIED_APPOINTMENT_TIER_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.toTelemetryMetricsMap
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.toTelemetryPropertiesMap
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.checkCaseloadAccess
import java.security.Principal
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSet as AppointmentSetModel

@Service
@Transactional
class AppointmentSetService(
  private val appointmentSetRepository: AppointmentSetRepository,
  private val appointmentTierRepository: AppointmentTierRepository,
  private val appointmentHostRepository: AppointmentHostRepository,
  private val referenceCodeService: ReferenceCodeService,
  private val locationService: LocationService,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val prisonApiClient: PrisonApiClient,
  private val transactionHandler: TransactionHandler,
  private val telemetryClient: TelemetryClient,
  private val auditService: AuditService,
) {
  @Transactional(readOnly = true)
  fun getAppointmentSetById(appointmentSetId: Long): AppointmentSetModel {
    val appointmentSet = appointmentSetRepository.findOrThrowNotFound(appointmentSetId)
    checkCaseloadAccess(appointmentSet.prisonCode)

    return appointmentSet.toModel()
  }

  @Transactional(readOnly = true)
  fun getAppointmentSetDetailsById(appointmentSetId: Long): AppointmentSetDetails {
    val appointmentSet = appointmentSetRepository.findOrThrowNotFound(appointmentSetId)
    checkCaseloadAccess(appointmentSet.prisonCode)

    val prisonerMap = prisonerSearchApiClient.findByPrisonerNumbersMap(appointmentSet.prisonerNumbers())

    val referenceCodeMap = referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY)

    val locationMap = locationService.getLocationsForAppointmentsMap(appointmentSet.prisonCode)

    val userMap = prisonApiClient.getUserDetailsList(appointmentSet.usernames()).associateBy { it.username }

    return appointmentSet.toDetails(prisonerMap, referenceCodeMap, locationMap, userMap)
  }

  fun createAppointmentSet(request: AppointmentSetCreateRequest, principal: Principal): AppointmentSetModel {
    val startTimeInMs = System.currentTimeMillis()

    val category = referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT)[request.categoryCode]
      ?: throw IllegalArgumentException("Appointment Category with code '${request.categoryCode}' not found or is not active")

    val locationMap = locationService.getLocationsForAppointmentsMap(request.prisonCode!!)[request.internalLocationId]
      ?: throw IllegalArgumentException("Appointment location with id '${request.internalLocationId}' not found in prison '${request.prisonCode}'")

    val prisonNumberBookingIdMap = createNumberBookingIdMap(request)
    request.appointments.map { it.prisonerNumber }.filterNot(prisonNumberBookingIdMap::containsKey).let {
      require(it.isEmpty()) {
        "Prisoner(s) with prisoner number(s) '${it.joinToString("', '")}' not found, were inactive or are residents of a different prison."
      }
    }

    val appointmentTier = appointmentTierRepository.findOrThrowNotFound(NOT_SPECIFIED_APPOINTMENT_TIER_ID)

    return transactionHandler.newSpringTransaction {
      appointmentSetRepository.saveAndFlush(
        request.toAppointmentSet(prisonNumberBookingIdMap, appointmentTier, principal.name),
      )
    }.also {
      // TODO: publish appointment instance created messages post transaction
      it.trackCreatedEvent(startTimeInMs, category.description, locationMap.userDescription ?: locationMap.description)
      it.auditCreatedEvent()
    }.toModel()
  }

  private fun createNumberBookingIdMap(request: AppointmentSetCreateRequest) =
    prisonerSearchApiClient.findByPrisonerNumbers(request.appointments.map { it.prisonerNumber!! })
      .filter { prisoner -> prisoner.prisonId == request.prisonCode }
      .associate { it.prisonerNumber to it.bookingId!!.toLong() }

  private fun AppointmentSetCreateRequest.toAppointmentSet(prisonNumberBookingIdMap: Map<String, Long>, appointmentTier: AppointmentTier, createdBy: String) =
    AppointmentSet(
      prisonCode = this.prisonCode!!,
      categoryCode = this.categoryCode!!,
      customName = this.customName,
      appointmentTier = appointmentTier,
      internalLocationId = this.internalLocationId,
      inCell = this.inCell,
      startDate = this.startDate!!,
      createdBy = createdBy,
    ).also { appointmentSet ->
      this.appointments.forEach { appointment ->
        appointmentSet.addAppointment(appointment, prisonNumberBookingIdMap)
      }
    }

  private fun AppointmentSet.addAppointment(appointment: AppointmentSetAppointment, prisonNumberBookingIdMap: Map<String, Long>) =
    this.addAppointmentSeries(
      AppointmentSeries(
        appointmentType = AppointmentType.INDIVIDUAL,
        prisonCode = this.prisonCode,
        categoryCode = this.categoryCode,
        customName = this.customName,
        appointmentTier = appointmentTier,
        internalLocationId = this.internalLocationId,
        inCell = this.inCell,
        startDate = this.startDate,
        startTime = appointment.startTime!!,
        endTime = appointment.endTime,
        extraInformation = appointment.extraInformation,
        createdTime = this.createdTime,
        createdBy = this.createdBy,
      ).apply {
        this.addAppointment(
          this.createAppointment(
            1,
            this.startDate,
          ).apply {
            this.addAttendee(
              AppointmentAttendee(
                appointment = this,
                prisonerNumber = appointment.prisonerNumber!!,
                bookingId = prisonNumberBookingIdMap[appointment.prisonerNumber]!!,
              ),
            )
          },
        )
      },
    )

  private fun AppointmentSet.trackCreatedEvent(
    startTimeInMs: Long,
    categoryDescription: String,
    internalLocationDescription: String,
  ) {
    val propertiesMap = this.toTelemetryPropertiesMap(categoryDescription, internalLocationDescription)
    val metricsMap = this.toTelemetryMetricsMap(startTimeInMs)
    telemetryClient.trackEvent(TelemetryEvent.APPOINTMENT_SET_CREATED.value, propertiesMap, metricsMap)
  }

  private fun AppointmentSet.auditCreatedEvent() {
    auditService.logEvent(
      AppointmentSetCreatedEvent(
        appointmentSetId = this.appointmentSetId,
        prisonCode = this.prisonCode,
        categoryCode = this.categoryCode,
        hasCustomName = this.customName != null,
        internalLocationId = this.internalLocationId,
        startDate = this.startDate,
        prisonerNumbers = this.prisonerNumbers(),
        createdAt = this.createdTime,
      ),
    )
  }
}
