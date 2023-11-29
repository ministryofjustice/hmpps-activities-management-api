package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentAttendee
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSeries
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSet
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.createAndAddAppointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSetDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentSetCreatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentSetAppointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentSetCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentSetRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.EventOrganiserRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.EventTierRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findByCodeOrThrowIllegalArgument
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EVENT_TIME_MS_METRIC_KEY
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
  private val eventTierRepository: EventTierRepository,
  private val eventOrganiserRepository: EventOrganiserRepository,
  private val referenceCodeService: ReferenceCodeService,
  private val locationService: LocationService,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val prisonApiClient: PrisonApiClient,
  private val transactionHandler: TransactionHandler,
  private val outboundEventsService: OutboundEventsService,
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

    checkCaseloadAccess(request.prisonCode!!)

    val categoryDescription = request.categoryDescription()
    val locationDescription = request.locationDescription()
    val prisonNumberBookingIdMap = request.createNumberBookingIdMap()
    request.failIfMissingPrisoners(prisonNumberBookingIdMap)

    transactionHandler.newSpringTransaction {
      appointmentSetRepository.saveAndFlush(
        request.toAppointmentSet(prisonNumberBookingIdMap, principal.name),
      )
    }.also {
      // TODO: publish appointment instance created messages post transaction
      it.appointments().forEach { appointment ->
        appointment.attendees().forEach { attendee ->
          outboundEventsService.send(OutboundEvent.APPOINTMENT_INSTANCE_CREATED, attendee.appointmentAttendeeId)
        }
      }
      it.auditCreatedEvent()

      val appointmentSetModel = it.toModel()
      appointmentSetModel.trackCreatedEvent(startTimeInMs, categoryDescription, locationDescription)
      return appointmentSetModel
    }
  }

  private fun AppointmentSetCreateRequest.categoryDescription() =
    referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT)[categoryCode]?.description
      ?: throw IllegalArgumentException("Appointment Category with code '$categoryCode' not found or is not active")

  private fun AppointmentSetCreateRequest.locationDescription(): String {
    return if (inCell) {
      "In cell"
    } else {
      locationService.getLocationsForAppointmentsMap(prisonCode!!)[internalLocationId]?.let { it.userDescription ?: it.description }
        ?: throw IllegalArgumentException("Appointment location with id '$internalLocationId' not found in prison '$prisonCode'")
    }
  }

  private fun AppointmentSetCreateRequest.createNumberBookingIdMap() =
    prisonerSearchApiClient.findByPrisonerNumbers(appointments.map { it.prisonerNumber!! })
      .filter { prisoner -> prisoner.prisonId == prisonCode }
      .associate { it.prisonerNumber to it.bookingId!!.toLong() }

  private fun AppointmentSetCreateRequest.failIfMissingPrisoners(prisonNumberBookingIdMap: Map<String, Long>) {
    appointments.map { it.prisonerNumber }.filterNot(prisonNumberBookingIdMap::containsKey).let {
      require(it.isEmpty()) {
        "Prisoner(s) with prisoner number(s) '${it.joinToString("', '")}' not found, were inactive or are residents of a different prison."
      }
    }
  }

  private fun AppointmentSetCreateRequest.toAppointmentSet(
    prisonNumberBookingIdMap: Map<String, Long>,
    createdBy: String,
  ): AppointmentSet {
    val tier = eventTierRepository.findByCodeOrThrowIllegalArgument(this.tierCode!!)
    val organiser = this.organiserCode?.let { eventOrganiserRepository.findByCodeOrThrowIllegalArgument(it) }

    return AppointmentSet(
      prisonCode = prisonCode!!,
      categoryCode = categoryCode!!,
      customName = customName?.trim()?.takeUnless(String::isBlank),
      appointmentTier = tier,
      internalLocationId = if (inCell) null else internalLocationId,
      inCell = inCell,
      startDate = startDate!!,
      createdBy = createdBy,
    ).also { appointmentSet ->
      appointmentSet.appointmentOrganiser = organiser
      appointments.forEach { appointment ->
        appointmentSet.addAppointment(appointment, prisonNumberBookingIdMap)
      }
    }
  }

  private fun AppointmentSet.addAppointment(appointment: AppointmentSetAppointment, prisonNumberBookingIdMap: Map<String, Long>) =
    addAppointmentSeries(
      AppointmentSeries(
        appointmentType = AppointmentType.INDIVIDUAL,
        prisonCode = prisonCode,
        categoryCode = categoryCode,
        customName = customName,
        appointmentTier = appointmentTier,
        internalLocationId = internalLocationId,
        inCell = inCell,
        startDate = startDate,
        startTime = appointment.startTime!!,
        endTime = appointment.endTime,
        extraInformation = appointment.extraInformation?.trim()?.takeUnless(String::isBlank),
        createdTime = createdTime,
        createdBy = createdBy,
      ).also {
        it.appointmentOrganiser = this.appointmentOrganiser

        it.createAndAddAppointment(
          1,
          startDate,
        ).apply {
          addAttendee(
            AppointmentAttendee(
              appointment = this,
              prisonerNumber = appointment.prisonerNumber!!,
              bookingId = prisonNumberBookingIdMap[appointment.prisonerNumber]!!,
            ),
          )
        }
      },
    )

  private fun AppointmentSetModel.trackCreatedEvent(
    startTimeInMs: Long,
    categoryDescription: String,
    locationDescription: String,
  ) {
    val propertiesMap = toTelemetryPropertiesMap(categoryDescription, locationDescription)
    val metricsMap = toTelemetryMetricsMap()
    metricsMap[EVENT_TIME_MS_METRIC_KEY] = (System.currentTimeMillis() - startTimeInMs).toDouble()
    telemetryClient.trackEvent(TelemetryEvent.APPOINTMENT_SET_CREATED.value, propertiesMap, metricsMap)
  }

  private fun AppointmentSet.auditCreatedEvent() {
    auditService.logEvent(
      AppointmentSetCreatedEvent(
        appointmentSetId = appointmentSetId,
        prisonCode = prisonCode,
        categoryCode = categoryCode,
        hasCustomName = customName != null,
        internalLocationId = internalLocationId,
        startDate = startDate,
        prisonerNumbers = prisonerNumbers(),
        createdAt = createdTime,
        createdBy = createdBy,
      ),
    )
  }
}
