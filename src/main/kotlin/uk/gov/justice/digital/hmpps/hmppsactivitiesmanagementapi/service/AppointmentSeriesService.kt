package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentFrequency
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSeriesSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.CreateAppointmentsJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentRepeat
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.BulkAppointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentSeriesCreatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentSetCreatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentMigrateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.BulkAppointmentsRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentCancellationReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentHostRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentSeriesRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentSetRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentTierRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.CANCELLED_APPOINTMENT_CANCELLATION_REASON_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.NOT_SPECIFIED_APPOINTMENT_TIER_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_INSTANCE_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_SERIES_ID_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_SET_ID_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.CATEGORY_CODE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.DESCRIPTION_LENGTH_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EARLIEST_START_TIME_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.END_TIME_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EVENT_TIME_MS_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EXTRA_INFORMATION_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EXTRA_INFORMATION_LENGTH_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.HAS_DESCRIPTION_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.HAS_EXTRA_INFORMATION_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.INTERNAL_LOCATION_ID_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.IS_REPEAT_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.LATEST_END_TIME_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISONER_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISON_CODE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.REPEAT_COUNT_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.REPEAT_PERIOD_PROPERTY_KEY
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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSet as AppointmentSetEntity

@Service
class AppointmentSeriesService(
  private val appointmentSeriesRepository: AppointmentSeriesRepository,
  private val appointmentTierRepository: AppointmentTierRepository,
  private val appointmentHostRepository: AppointmentHostRepository,
  private val appointmentCancellationReasonRepository: AppointmentCancellationReasonRepository,
  private val appointmentSetRepository: AppointmentSetRepository,
  private val referenceCodeService: ReferenceCodeService,
  private val locationService: LocationService,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val createAppointmentsJob: CreateAppointmentsJob,
  private val telemetryClient: TelemetryClient,
  private val auditService: AuditService,
  @Value("\${applications.max-appointment-instances}") private val maxAppointmentInstances: Int = 20000,
  @Value("\${applications.max-sync-appointment-instance-actions}") private val maxSyncAppointmentInstanceActions: Int = 500,
) {
  @Transactional(readOnly = true)
  fun getAppointmentSeriesById(appointmentSeriesId: Long): Appointment {
    val appointmentSeries = appointmentSeriesRepository.findOrThrowNotFound(appointmentSeriesId).toModel()
    checkCaseloadAccess(appointmentSeries.prisonCode)

    return appointmentSeries
  }

  // TODO: Create AppointmentSetService and move this function
  fun createAppointmentSet(request: BulkAppointmentsRequest, principal: Principal): BulkAppointment {
    val startTime = System.currentTimeMillis()

    val appointmentTier = appointmentTierRepository.findOrThrowNotFound(NOT_SPECIFIED_APPOINTMENT_TIER_ID)

    return require(request.appointments.isNotEmpty()) { "One or more appointments must be supplied." }.run {
      createPrisonerMap(request.appointments.map { it.prisonerNumber }, request.prisonCode).let { prisonerBookings ->
        AppointmentSetEntity(
          prisonCode = request.prisonCode,
          categoryCode = request.categoryCode,
          customName = request.appointmentDescription,
          appointmentTier = appointmentTier,
          internalLocationId = request.internalLocationId,
          inCell = request.inCell,
          startDate = request.startDate,
          createdBy = principal.name,
        ).apply {
          request.appointments.map {
            buildValidAppointmentSeriesEntity(
              appointmentType = AppointmentType.INDIVIDUAL,
              prisonCode = request.prisonCode,
              prisonerNumbers = listOf(it.prisonerNumber),
              prisonerBookings = prisonerBookings.filterKeys { k -> k == it.prisonerNumber },
              categoryCode = request.categoryCode,
              customName = request.appointmentDescription,
              appointmentTier = appointmentTier,
              internalLocationId = request.internalLocationId,
              inCell = request.inCell,
              startDate = request.startDate,
              startTime = it.startTime,
              endTime = it.endTime,
              extraInformation = it.comment,
              createdBy = principal.name,
            )
          }.forEach { appointmentSeries -> this.addAppointmentSeries(appointmentSeries) }
        }.let { appointmentSetRepository.saveAndFlush(it).toModel() }
          .also {
            logAppointmentSetCreatedMetric(principal, it, startTime)
            writeAppointmentSetCreatedAuditRecord(request, it)
          }
      }
    }
  }

  fun createAppointmentSeries(request: AppointmentCreateRequest, principal: Principal): Appointment {
    val startTime = System.currentTimeMillis()

    val appointmentTier = appointmentTierRepository.findOrThrowNotFound(NOT_SPECIFIED_APPOINTMENT_TIER_ID)

    val prisonerBookings = createPrisonerMap(request.prisonerNumbers, request.prisonCode)
    // Determine if this is a create request for a very large appointment series. If it is, this function will only create the first appointment
    val createFirstAppointmentOnly = request.repeat?.count?.let { it > 1 && it * prisonerBookings.size > maxSyncAppointmentInstanceActions } ?: false

    val appointmentSeries = appointmentSeriesRepository.saveAndFlush(
      buildValidAppointmentSeriesEntity(
        appointmentType = request.appointmentType,
        prisonCode = request.prisonCode!!,
        prisonerNumbers = request.prisonerNumbers,
        prisonerBookings = prisonerBookings,
        inCell = request.inCell,
        categoryCode = request.categoryCode!!,
        customName = request.appointmentDescription,
        appointmentTier = appointmentTier,
        internalLocationId = request.internalLocationId,
        startDate = request.startDate,
        startTime = request.startTime,
        endTime = request.endTime,
        repeat = request.repeat,
        extraInformation = request.comment,
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

  fun migrateAppointment(request: AppointmentMigrateRequest, principal: Principal): Appointment {
    val appointmentTier = appointmentTierRepository.findOrThrowNotFound(NOT_SPECIFIED_APPOINTMENT_TIER_ID)
    val prisonerBookings = mapOf(request.prisonerNumber!! to request.bookingId.toString())
    return buildValidAppointmentSeriesEntity(
      appointmentType = AppointmentType.INDIVIDUAL,
      prisonCode = request.prisonCode!!,
      prisonerNumbers = listOf(request.prisonerNumber),
      prisonerBookings = prisonerBookings,
      categoryCode = request.categoryCode!!,
      appointmentTier = appointmentTier,
      internalLocationId = request.internalLocationId,
      inCell = false,
      startDate = request.startDate,
      startTime = request.startTime,
      endTime = request.endTime,
      extraInformation = request.comment,
      createdTime = request.created!!,
      createdBy = request.createdBy!!,
      updatedTime = request.updated,
      updatedBy = request.updatedBy,
      isCancelled = request.isCancelled ?: false,
      isMigrated = true,
    ).let { (appointmentSeriesRepository.saveAndFlush(it)).toModel() }
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

  private fun failIfMaximumAppointmentInstancesExceeded(prisonerNumbers: List<String>, repeat: AppointmentRepeat?) {
    val repeatCount = repeat?.count ?: 1
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
    repeat: AppointmentRepeat? = null,
    extraInformation: String? = null,
    createdTime: LocalDateTime = LocalDateTime.now(),
    createdBy: String,
    updatedTime: LocalDateTime? = null,
    updatedBy: String? = null,
    isCancelled: Boolean = false,
    isMigrated: Boolean = false,
    createFirstAppointmentOnly: Boolean = false,
  ): AppointmentSeriesEntity {
    failIfMaximumAppointmentInstancesExceeded(prisonerNumbers, repeat)

    if (!isMigrated) {
      checkCaseloadAccess(prisonCode)
      failIfCategoryNotFound(categoryCode)
      failIfLocationNotFound(inCell, prisonCode, internalLocationId)
      failIfMissingPrisoners(prisonerNumbers, prisonerBookings)
    }

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
      isMigrated = isMigrated,
    ).apply {
      this.schedule = repeat?.let {
        AppointmentSeriesSchedule(
          appointmentSeries = this,
          frequency = AppointmentFrequency.valueOf(repeat.period!!.name),
          numberOfAppointments = repeat.count!!,
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
    prisonerSearchApiClient.findByPrisonerNumbers(prisonerNumbers).block()!!
      .filter { prisoner -> prisoner.prisonId == prisonCode }
      .associate { it.prisonerNumber to it.bookingId }

  private fun logAppointmentSeriesCreatedMetric(principal: Principal, request: AppointmentCreateRequest, appointmentSeries: Appointment, startTimeInMs: Long) {
    val propertiesMap = mapOf(
      USER_PROPERTY_KEY to principal.name,
      PRISON_CODE_PROPERTY_KEY to appointmentSeries.prisonCode,
      APPOINTMENT_SERIES_ID_PROPERTY_KEY to appointmentSeries.id.toString(),
      CATEGORY_CODE_PROPERTY_KEY to appointmentSeries.categoryCode,
      HAS_DESCRIPTION_PROPERTY_KEY to (appointmentSeries.appointmentDescription?.isNotEmpty()).toString(),
      INTERNAL_LOCATION_ID_PROPERTY_KEY to appointmentSeries.internalLocationId.toString(),
      START_DATE_PROPERTY_KEY to appointmentSeries.startDate.toString(),
      START_TIME_PROPERTY_KEY to appointmentSeries.startTime.toString(),
      END_TIME_PROPERTY_KEY to appointmentSeries.endTime.toString(),
      IS_REPEAT_PROPERTY_KEY to (request.repeat != null).toString(),
      REPEAT_PERIOD_PROPERTY_KEY to (request.repeat?.period?.toString() ?: ""),
      REPEAT_COUNT_PROPERTY_KEY to (request.repeat?.count?.toString() ?: ""),
      HAS_EXTRA_INFORMATION_PROPERTY_KEY to (appointmentSeries.comment?.isNotEmpty() == true).toString(),
    )

    val metricsMap = mapOf(
      PRISONER_COUNT_METRIC_KEY to request.prisonerNumbers.size.toDouble(),
      APPOINTMENT_INSTANCE_COUNT_METRIC_KEY to (request.prisonerNumbers.size * (request.repeat?.count ?: 1)).toDouble(),
      DESCRIPTION_LENGTH_METRIC_KEY to (appointmentSeries.appointmentDescription?.length ?: 0).toDouble(),
      EXTRA_INFORMATION_LENGTH_METRIC_KEY to (appointmentSeries.comment?.length ?: 0).toDouble(),
      EVENT_TIME_MS_METRIC_KEY to (System.currentTimeMillis() - startTimeInMs).toDouble(),
    )

    telemetryClient.trackEvent(TelemetryEvent.APPOINTMENT_SERIES_CREATED.value, propertiesMap, metricsMap)
  }

  private fun logAppointmentSetCreatedMetric(principal: Principal, appointmentSet: BulkAppointment, startTimeInMs: Long) {
    val propertiesMap = mapOf(
      USER_PROPERTY_KEY to principal.name,
      PRISON_CODE_PROPERTY_KEY to appointmentSet.prisonCode,
      APPOINTMENT_SET_ID_PROPERTY_KEY to appointmentSet.id.toString(),
      CATEGORY_CODE_PROPERTY_KEY to appointmentSet.categoryCode,
      HAS_DESCRIPTION_PROPERTY_KEY to (appointmentSet.appointmentDescription != null).toString(),
      INTERNAL_LOCATION_ID_PROPERTY_KEY to appointmentSet.internalLocationId.toString(),
      START_DATE_PROPERTY_KEY to appointmentSet.startDate.toString(),
      EARLIEST_START_TIME_PROPERTY_KEY to appointmentSet.appointments.minOf { it.startTime }.toString(),
      LATEST_END_TIME_PROPERTY_KEY to appointmentSet.appointments.mapNotNull { it.endTime }.maxOf { it }.toString(),
    )

    val metricsMap = mapOf(
      APPOINTMENT_COUNT_METRIC_KEY to appointmentSet.appointments.size.toDouble(),
      APPOINTMENT_INSTANCE_COUNT_METRIC_KEY to appointmentSet.appointments.flatMap { it.occurrences.flatMap { appointment -> appointment.allocations } }.size.toDouble(),
      DESCRIPTION_LENGTH_METRIC_KEY to (appointmentSet.appointmentDescription?.length ?: 0).toDouble(),
      EXTRA_INFORMATION_COUNT_METRIC_KEY to appointmentSet.appointments.filterNot { it.comment.isNullOrEmpty() }.size.toDouble(),
      EVENT_TIME_MS_METRIC_KEY to (System.currentTimeMillis() - startTimeInMs).toDouble(),
    )

    telemetryClient.trackEvent(TelemetryEvent.APPOINTMENT_SET_CREATED.value, propertiesMap, metricsMap)
  }

  private fun writeAppointmentCreatedAuditRecord(request: AppointmentCreateRequest, appointment: Appointment) {
    auditService.logEvent(
      AppointmentSeriesCreatedEvent(
        appointmentSeriesId = appointment.id,
        prisonCode = appointment.prisonCode,
        categoryCode = appointment.categoryCode,
        hasCustomName = appointment.appointmentDescription != null,
        internalLocationId = appointment.internalLocationId,
        startDate = appointment.startDate,
        startTime = appointment.startTime,
        endTime = appointment.endTime,
        isRepeat = request.repeat != null,
        frequency = request.repeat?.period,
        numberOfAppointments = request.repeat?.count,
        hasExtraInformation = appointment.comment?.isNotEmpty() == true,
        prisonerNumbers = request.prisonerNumbers,
        createdTime = LocalDateTime.now(),
      ),
    )
  }
  private fun writeAppointmentSetCreatedAuditRecord(request: BulkAppointmentsRequest, appointment: BulkAppointment) {
    auditService.logEvent(
      AppointmentSetCreatedEvent(
        appointmentSetId = appointment.id,
        prisonCode = appointment.prisonCode,
        categoryCode = appointment.categoryCode,
        hasCustomName = appointment.appointmentDescription != null,
        internalLocationId = appointment.internalLocationId,
        startDate = appointment.startDate,
        prisonerNumbers = request.appointments.map { it.prisonerNumber },
        createdAt = LocalDateTime.now(),
      ),
    )
  }
}
