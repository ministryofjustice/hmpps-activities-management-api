package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentFrequency
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.CreateAppointmentsJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeries
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeriesDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeriesSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentSeriesCreatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentSeriesCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentCancellationReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentHostRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentSeriesRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentTierRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.CANCELLED_APPOINTMENT_CANCELLATION_REASON_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.NOT_SPECIFIED_APPOINTMENT_TIER_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_INSTANCE_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_SERIES_ID_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.CATEGORY_CODE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.CUSTOM_NAME_LENGTH_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.END_TIME_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EVENT_TIME_MS_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EXTRA_INFORMATION_LENGTH_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.FREQUENCY_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.HAS_CUSTOM_NAME_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.HAS_EXTRA_INFORMATION_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.INTERNAL_LOCATION_ID_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.IS_REPEAT_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.NUMBER_OF_APPOINTMENTS_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISONER_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISON_CODE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.START_DATE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.START_TIME_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.USER_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.checkCaseloadAccess
import java.security.Principal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Appointment as AppointmentEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentAttendee as AppointmentAttendeeEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSeries as AppointmentSeriesEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSeriesSchedule as AppointmentSeriesScheduleEntity

@Service
class AppointmentSeriesService(
  private val appointmentSeriesRepository: AppointmentSeriesRepository,
  private val appointmentTierRepository: AppointmentTierRepository,
  private val appointmentHostRepository: AppointmentHostRepository,
  private val appointmentCancellationReasonRepository: AppointmentCancellationReasonRepository,
  private val referenceCodeService: ReferenceCodeService,
  private val locationService: LocationService,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val prisonApiClient: PrisonApiClient,
  private val createAppointmentsJob: CreateAppointmentsJob,
  private val telemetryClient: TelemetryClient,
  private val auditService: AuditService,
  @Value("\${applications.max-appointment-instances}") private val maxAppointmentInstances: Int = 20000,
  @Value("\${applications.max-sync-appointment-instance-actions}") private val maxSyncAppointmentInstanceActions: Int = 500,
) {
  @Transactional(readOnly = true)
  fun getAppointmentSeriesById(appointmentSeriesId: Long): AppointmentSeries {
    val appointmentSeries = appointmentSeriesRepository.findOrThrowNotFound(appointmentSeriesId)
    checkCaseloadAccess(appointmentSeries.prisonCode)

    return appointmentSeries.toModel()
  }

  @Transactional(readOnly = true)
  fun getAppointmentSeriesDetailsById(appointmentSeriesId: Long): AppointmentSeriesDetails {
    val appointmentSeries = appointmentSeriesRepository.findOrThrowNotFound(appointmentSeriesId)
    checkCaseloadAccess(appointmentSeries.prisonCode)

    val referenceCodeMap = referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY)

    val locationMap = locationService.getLocationsForAppointmentsMap(appointmentSeries.prisonCode)

    val userMap = prisonApiClient.getUserDetailsList(appointmentSeries.usernames()).associateBy { it.username }

    return appointmentSeries.toDetails(referenceCodeMap, locationMap, userMap)
  }

  fun createAppointmentSeries(request: AppointmentSeriesCreateRequest, principal: Principal): AppointmentSeries {
    val startTime = System.currentTimeMillis()

    val appointmentTier = appointmentTierRepository.findOrThrowNotFound(NOT_SPECIFIED_APPOINTMENT_TIER_ID)

    val prisonerBookings = createPrisonerMap(request.prisonerNumbers, request.prisonCode)
    // Determine if this is a create request for a very large appointment series. If it is, this function will only create the first appointment
    val createFirstAppointmentOnly = request.schedule?.numberOfAppointments?.let { it > 1 && it * prisonerBookings.size > maxSyncAppointmentInstanceActions } ?: false

    val appointmentSeries = appointmentSeriesRepository.saveAndFlush(
      buildValidAppointmentSeriesEntity(
        appointmentType = request.appointmentType,
        prisonCode = request.prisonCode!!,
        prisonerNumbers = request.prisonerNumbers,
        prisonerBookings = prisonerBookings,
        inCell = request.inCell,
        categoryCode = request.categoryCode!!,
        customName = request.customName,
        appointmentTier = appointmentTier,
        internalLocationId = request.internalLocationId,
        startDate = request.startDate,
        startTime = request.startTime,
        endTime = request.endTime,
        repeat = request.schedule,
        extraInformation = request.extraInformation,
        createdBy = principal.name,
        createFirstAppointmentOnly = createFirstAppointmentOnly,
      ),
    )

    if (createFirstAppointmentOnly) {
      // The remaining appointments will be created asynchronously by this job
      createAppointmentsJob.execute(appointmentSeries.appointmentSeriesId, prisonerBookings)
    }

    return appointmentSeries.toModel().also {
      logAppointmentSeriesCreatedMetric(principal, request, it, startTime)
      writeAppointmentCreatedAuditRecord(request, it)
    }
  }

  private fun failIfCategoryNotFound(categoryCode: String) {
    referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT)[categoryCode]
      ?: throw IllegalArgumentException("Appointment Category with code $categoryCode not found or is not active")
  }

  private fun failIfLocationNotFound(inCell: Boolean, prisonCode: String?, internalLocationId: Long?) {
    if (!inCell) {
      locationService.getLocationsForAppointmentsMap(prisonCode!!)[internalLocationId]
        ?: throw IllegalArgumentException("Appointment location with id $internalLocationId not found in prison '$prisonCode'")
    }
  }

  private fun failIfMissingPrisoners(prisonerNumbers: List<String>, prisonerBookings: Map<String, String?>) {
    prisonerNumbers.filterNot(prisonerBookings::containsKey).let {
      require(it.isEmpty()) {
        "Prisoner(s) with prisoner number(s) '${it.joinToString("', '")}' not found, were inactive or are residents of a different prison."
      }
    }
  }

  private fun failIfMaximumAppointmentInstancesExceeded(prisonerNumbers: List<String>, repeat: AppointmentSeriesSchedule?) {
    val repeatCount = repeat?.numberOfAppointments ?: 1
    require(prisonerNumbers.size * repeatCount <= maxAppointmentInstances) {
      "You cannot schedule more than ${maxAppointmentInstances / prisonerNumbers.size} appointments for this number of attendees."
    }
  }

  fun buildValidAppointmentSeriesEntity(
    appointmentType: AppointmentType? = null,
    prisonCode: String,
    prisonerNumbers: List<String>,
    prisonerBookings: Map<String, String?>,
    categoryCode: String,
    customName: String? = null,
    appointmentTier: AppointmentTier,
    internalLocationId: Long? = null,
    inCell: Boolean = false,
    startDate: LocalDate?,
    startTime: LocalTime?,
    endTime: LocalTime?,
    repeat: AppointmentSeriesSchedule? = null,
    extraInformation: String? = null,
    createdTime: LocalDateTime = LocalDateTime.now(),
    createdBy: String,
    updatedTime: LocalDateTime? = null,
    updatedBy: String? = null,
    isCancelled: Boolean = false,
    createFirstAppointmentOnly: Boolean = false,
  ): AppointmentSeriesEntity {
    failIfMaximumAppointmentInstancesExceeded(prisonerNumbers, repeat)

    checkCaseloadAccess(prisonCode)
    failIfCategoryNotFound(categoryCode)
    failIfLocationNotFound(inCell, prisonCode, internalLocationId)
    failIfMissingPrisoners(prisonerNumbers, prisonerBookings)

    return AppointmentSeriesEntity(
      appointmentType = appointmentType!!,
      prisonCode = prisonCode,
      categoryCode = categoryCode,
      customName = customName?.takeUnless(String::isBlank),
      appointmentTier = appointmentTier,
      internalLocationId = if (inCell) null else internalLocationId,
      inCell = inCell,
      startDate = startDate!!,
      startTime = startTime!!,
      endTime = endTime,
      extraInformation = extraInformation,
      createdTime = createdTime,
      createdBy = createdBy,
      updatedTime = updatedTime,
      updatedBy = updatedBy,
    ).apply {
      this.schedule = repeat?.let {
        AppointmentSeriesScheduleEntity(
          appointmentSeries = this,
          frequency = AppointmentFrequency.valueOf(repeat.frequency!!.name),
          numberOfAppointments = repeat.numberOfAppointments!!,
        )
      }

      this.scheduleIterator().withIndex().forEach {
        if (createFirstAppointmentOnly && it.index > 0) return@forEach

        this.addAppointment(
          AppointmentEntity(
            appointmentSeries = this,
            sequenceNumber = it.index + 1,
            prisonCode = this.prisonCode,
            categoryCode = this.categoryCode,
            customName = this.customName,
            internalLocationId = this.internalLocationId,
            appointmentTier = this.appointmentTier,
            inCell = this.inCell,
            startDate = it.value,
            startTime = this.startTime,
            endTime = this.endTime,
            extraInformation = this.extraInformation,
            createdTime = this.createdTime,
            createdBy = this.createdBy,
            updatedTime = this.updatedTime,
            updatedBy = this.updatedBy,
          ).apply {
            prisonerBookings.forEach { prisonerBooking ->
              this.addAttendee(
                AppointmentAttendeeEntity(
                  appointment = this,
                  prisonerNumber = prisonerBooking.key,
                  bookingId = prisonerBooking.value!!.toLong(),
                ),
              )
            }

            if (isCancelled) {
              cancelledTime = updatedTime ?: createdTime
              cancellationReason =
                appointmentCancellationReasonRepository.findOrThrowNotFound(CANCELLED_APPOINTMENT_CANCELLATION_REASON_ID)
              cancelledBy = updatedBy ?: createdBy
            }
          },
        )
      }
    }
  }

  private fun createPrisonerMap(prisonerNumbers: List<String>, prisonCode: String?) =
    prisonerSearchApiClient.findByPrisonerNumbers(prisonerNumbers)
      .filter { prisoner -> prisoner.prisonId == prisonCode }
      .associate { it.prisonerNumber to it.bookingId }

  private fun logAppointmentSeriesCreatedMetric(principal: Principal, request: AppointmentSeriesCreateRequest, appointmentSeries: AppointmentSeries, startTimeInMs: Long) {
    val propertiesMap = mapOf(
      USER_PROPERTY_KEY to principal.name,
      PRISON_CODE_PROPERTY_KEY to appointmentSeries.prisonCode,
      APPOINTMENT_SERIES_ID_PROPERTY_KEY to appointmentSeries.id.toString(),
      CATEGORY_CODE_PROPERTY_KEY to appointmentSeries.categoryCode,
      HAS_CUSTOM_NAME_PROPERTY_KEY to (appointmentSeries.customName?.isNotEmpty()).toString(),
      INTERNAL_LOCATION_ID_PROPERTY_KEY to appointmentSeries.internalLocationId.toString(),
      START_DATE_PROPERTY_KEY to appointmentSeries.startDate.toString(),
      START_TIME_PROPERTY_KEY to appointmentSeries.startTime.toString(),
      END_TIME_PROPERTY_KEY to appointmentSeries.endTime.toString(),
      IS_REPEAT_PROPERTY_KEY to (request.schedule != null).toString(),
      FREQUENCY_PROPERTY_KEY to (request.schedule?.frequency?.toString() ?: ""),
      NUMBER_OF_APPOINTMENTS_PROPERTY_KEY to (request.schedule?.numberOfAppointments?.toString() ?: ""),
      HAS_EXTRA_INFORMATION_PROPERTY_KEY to (appointmentSeries.extraInformation?.isNotEmpty() == true).toString(),
    )

    val metricsMap = mapOf(
      PRISONER_COUNT_METRIC_KEY to request.prisonerNumbers.size.toDouble(),
      APPOINTMENT_COUNT_METRIC_KEY to (request.schedule?.numberOfAppointments ?: 1).toDouble(),
      APPOINTMENT_INSTANCE_COUNT_METRIC_KEY to (request.prisonerNumbers.size * (request.schedule?.numberOfAppointments ?: 1)).toDouble(),
      CUSTOM_NAME_LENGTH_METRIC_KEY to (appointmentSeries.customName?.length ?: 0).toDouble(),
      EXTRA_INFORMATION_LENGTH_METRIC_KEY to (appointmentSeries.extraInformation?.length ?: 0).toDouble(),
      EVENT_TIME_MS_METRIC_KEY to (System.currentTimeMillis() - startTimeInMs).toDouble(),
    )

    telemetryClient.trackEvent(TelemetryEvent.APPOINTMENT_SERIES_CREATED.value, propertiesMap, metricsMap)
  }

  private fun writeAppointmentCreatedAuditRecord(request: AppointmentSeriesCreateRequest, appointmentSeries: AppointmentSeries) {
    auditService.logEvent(
      AppointmentSeriesCreatedEvent(
        appointmentSeriesId = appointmentSeries.id,
        prisonCode = appointmentSeries.prisonCode,
        categoryCode = appointmentSeries.categoryCode,
        hasCustomName = appointmentSeries.customName != null,
        internalLocationId = appointmentSeries.internalLocationId,
        startDate = appointmentSeries.startDate,
        startTime = appointmentSeries.startTime,
        endTime = appointmentSeries.endTime,
        isRepeat = request.schedule != null,
        frequency = request.schedule?.frequency,
        numberOfAppointments = request.schedule?.numberOfAppointments,
        hasExtraInformation = appointmentSeries.extraInformation?.isNotEmpty() == true,
        prisonerNumbers = request.prisonerNumbers,
        createdTime = LocalDateTime.now(),
      ),
    )
  }
}
