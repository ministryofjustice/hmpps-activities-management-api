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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.CreateAppointmentOccurrencesJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentRepeat
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.BulkAppointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentCreatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.BulkAppointmentCreatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentMigrateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.BulkAppointmentsRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentCancellationReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentHostRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentTierRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.BulkAppointmentRepository
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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSeries as AppointmentEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Appointment as AppointmentOccurrenceEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentAttendee as AppointmentOccurrenceAllocationEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSet as BulkAppointmentEntity

@Service
class AppointmentService(
  private val appointmentRepository: AppointmentRepository,
  private val appointmentTierRepository: AppointmentTierRepository,
  private val appointmentHostRepository: AppointmentHostRepository,
  private val appointmentCancellationReasonRepository: AppointmentCancellationReasonRepository,
  private val bulkAppointmentRepository: BulkAppointmentRepository,
  private val referenceCodeService: ReferenceCodeService,
  private val locationService: LocationService,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val createAppointmentOccurrencesJob: CreateAppointmentOccurrencesJob,
  private val telemetryClient: TelemetryClient,
  private val auditService: AuditService,
  @Value("\${applications.max-appointment-instances}") private val maxAppointmentInstances: Int = 20000,
  @Value("\${applications.max-sync-appointment-instance-actions}") private val maxSyncAppointmentInstanceActions: Int = 500,
) {
  @Transactional(readOnly = true)
  fun getAppointmentById(appointmentId: Long): Appointment {
    val appointmentSeries = appointmentRepository.findOrThrowNotFound(appointmentId).toModel()
    checkCaseloadAccess(appointmentSeries.prisonCode)

    return appointmentSeries
  }

  fun bulkCreateAppointments(request: BulkAppointmentsRequest, principal: Principal): BulkAppointment {
    val startTime = System.currentTimeMillis()

    val appointmentTier = appointmentTierRepository.findOrThrowNotFound(NOT_SPECIFIED_APPOINTMENT_TIER_ID)

    return require(request.appointments.isNotEmpty()) { "One or more appointments must be supplied." }.run {
      createPrisonerMap(request.appointments.map { it.prisonerNumber }, request.prisonCode).let { prisonerBookings ->
        BulkAppointmentEntity(
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
            buildValidAppointmentEntity(
              appointmentType = AppointmentType.INDIVIDUAL,
              prisonCode = request.prisonCode,
              prisonerNumbers = listOf(it.prisonerNumber),
              prisonerBookings = prisonerBookings.filterKeys { k -> k == it.prisonerNumber },
              categoryCode = request.categoryCode,
              appointmentDescription = request.appointmentDescription,
              appointmentTier = appointmentTier,
              internalLocationId = request.internalLocationId,
              inCell = request.inCell,
              startDate = request.startDate,
              startTime = it.startTime,
              endTime = it.endTime,
              comment = it.comment,
              createdBy = principal.name,
            )
          }.forEach { appointment -> this.addAppointmentSeries(appointment) }
        }.let { bulkAppointmentRepository.saveAndFlush(it).toModel() }
          .also {
            logBulkAppointmentCreatedMetric(principal, it, startTime)
            writeBulkAppointmentCreatedAuditRecord(request, it)
          }
      }
    }
  }

  fun createAppointment(request: AppointmentCreateRequest, principal: Principal): Appointment {
    val startTime = System.currentTimeMillis()

    val appointmentTier = appointmentTierRepository.findOrThrowNotFound(NOT_SPECIFIED_APPOINTMENT_TIER_ID)

    val prisonerBookings = createPrisonerMap(request.prisonerNumbers, request.prisonCode)
    // Determine if this is a create request for a very large appointment. If it is, this function will only create the first occurrence
    val createFirstOccurrenceOnly = request.repeat?.count?.let { it > 1 && it * prisonerBookings.size > maxSyncAppointmentInstanceActions } ?: false

    val appointmentSeries = appointmentRepository.saveAndFlush(
      buildValidAppointmentEntity(
        appointmentType = request.appointmentType,
        prisonCode = request.prisonCode!!,
        prisonerNumbers = request.prisonerNumbers,
        prisonerBookings = prisonerBookings,
        inCell = request.inCell,
        categoryCode = request.categoryCode!!,
        appointmentDescription = request.appointmentDescription,
        appointmentTier = appointmentTier,
        internalLocationId = request.internalLocationId,
        startDate = request.startDate,
        startTime = request.startTime,
        endTime = request.endTime,
        repeat = request.repeat,
        comment = request.comment,
        createdBy = principal.name,
        createFirstOccurrenceOnly = createFirstOccurrenceOnly,
      ),
    )

    if (createFirstOccurrenceOnly) {
      // The remaining occurrences will be created asynchronously by this job
      createAppointmentOccurrencesJob.execute(appointmentSeries.appointmentSeriesId, prisonerBookings)
    }

    return appointmentSeries.toModel().also {
      logAppointmentCreatedMetric(principal, request, it, startTime)
      writeAppointmentCreatedAuditRecord(request, it)
    }
  }

  fun migrateAppointment(request: AppointmentMigrateRequest, principal: Principal): Appointment {
    val appointmentTier = appointmentTierRepository.findOrThrowNotFound(NOT_SPECIFIED_APPOINTMENT_TIER_ID)
    val prisonerBookings = mapOf(request.prisonerNumber!! to request.bookingId.toString())
    return buildValidAppointmentEntity(
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
      comment = request.comment,
      created = request.created!!,
      createdBy = request.createdBy!!,
      updated = request.updated,
      updatedBy = request.updatedBy,
      isCancelled = request.isCancelled ?: false,
      isMigrated = true,
    ).let { (appointmentRepository.saveAndFlush(it)).toModel() }
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

  private fun failIfMaximumOccurrencesExceeded(prisonerNumbers: List<String>, repeat: AppointmentRepeat?) {
    val repeatCount = repeat?.count ?: 1
    require(prisonerNumbers.size * repeatCount <= maxAppointmentInstances) {
      "You cannot schedule more than ${maxAppointmentInstances / prisonerNumbers.size} appointments for this number of attendees."
    }
  }

  fun buildValidAppointmentEntity(
    appointmentType: AppointmentType? = null,
    prisonCode: String,
    prisonerNumbers: List<String>,
    prisonerBookings: Map<String, String?>,
    categoryCode: String,
    appointmentDescription: String? = null,
    appointmentTier: AppointmentTier,
    internalLocationId: Long? = null,
    inCell: Boolean = false,
    startDate: LocalDate?,
    startTime: LocalTime?,
    endTime: LocalTime?,
    repeat: AppointmentRepeat? = null,
    comment: String? = null,
    created: LocalDateTime = LocalDateTime.now(),
    createdBy: String,
    updated: LocalDateTime? = null,
    updatedBy: String? = null,
    isCancelled: Boolean = false,
    isMigrated: Boolean = false,
    createFirstOccurrenceOnly: Boolean = false,
  ): AppointmentEntity {
    failIfMaximumOccurrencesExceeded(prisonerNumbers, repeat)

    if (!isMigrated) {
      checkCaseloadAccess(prisonCode)
      failIfCategoryNotFound(categoryCode)
      failIfLocationNotFound(inCell, prisonCode, internalLocationId)
      failIfMissingPrisoners(prisonerNumbers, prisonerBookings)
    }

    return AppointmentEntity(
      appointmentType = appointmentType!!,
      prisonCode = prisonCode,
      categoryCode = categoryCode,
      customName = appointmentDescription?.takeUnless(String::isBlank),
      appointmentTier = appointmentTier,
      internalLocationId = if (inCell) null else internalLocationId,
      inCell = inCell,
      startDate = startDate!!,
      startTime = startTime!!,
      endTime = endTime,
      extraInformation = comment,
      createdTime = created,
      createdBy = createdBy,
      updatedTime = updated,
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
        if (createFirstOccurrenceOnly && it.index > 0) return@forEach

        this.addAppointment(
          AppointmentOccurrenceEntity(
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
                AppointmentOccurrenceAllocationEntity(
                  appointment = this,
                  prisonerNumber = prisonerBooking.key,
                  bookingId = prisonerBooking.value!!.toLong(),
                ),
              )
            }

            if (isCancelled) {
              cancelledTime = updated ?: created
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

  private fun logAppointmentCreatedMetric(principal: Principal, request: AppointmentCreateRequest, appointment: Appointment, startTimeInMs: Long) {
    val propertiesMap = mapOf(
      USER_PROPERTY_KEY to principal.name,
      PRISON_CODE_PROPERTY_KEY to appointment.prisonCode,
      APPOINTMENT_SERIES_ID_PROPERTY_KEY to appointment.id.toString(),
      CATEGORY_CODE_PROPERTY_KEY to appointment.categoryCode,
      HAS_DESCRIPTION_PROPERTY_KEY to (appointment.appointmentDescription?.isNotEmpty()).toString(),
      INTERNAL_LOCATION_ID_PROPERTY_KEY to appointment.internalLocationId.toString(),
      START_DATE_PROPERTY_KEY to appointment.startDate.toString(),
      START_TIME_PROPERTY_KEY to appointment.startTime.toString(),
      END_TIME_PROPERTY_KEY to appointment.endTime.toString(),
      IS_REPEAT_PROPERTY_KEY to (request.repeat != null).toString(),
      REPEAT_PERIOD_PROPERTY_KEY to (request.repeat?.period?.toString() ?: ""),
      REPEAT_COUNT_PROPERTY_KEY to (request.repeat?.count?.toString() ?: ""),
      HAS_EXTRA_INFORMATION_PROPERTY_KEY to (appointment.comment?.isNotEmpty() == true).toString(),
    )

    val metricsMap = mapOf(
      PRISONER_COUNT_METRIC_KEY to request.prisonerNumbers.size.toDouble(),
      APPOINTMENT_INSTANCE_COUNT_METRIC_KEY to (request.prisonerNumbers.size * (request.repeat?.count ?: 1)).toDouble(),
      DESCRIPTION_LENGTH_METRIC_KEY to (appointment.appointmentDescription?.length ?: 0).toDouble(),
      EXTRA_INFORMATION_LENGTH_METRIC_KEY to (appointment.comment?.length ?: 0).toDouble(),
      EVENT_TIME_MS_METRIC_KEY to (System.currentTimeMillis() - startTimeInMs).toDouble(),
    )

    telemetryClient.trackEvent(TelemetryEvent.APPOINTMENT_CREATED.value, propertiesMap, metricsMap)
  }

  private fun logBulkAppointmentCreatedMetric(principal: Principal, appointment: BulkAppointment, startTimeInMs: Long) {
    val propertiesMap = mapOf(
      USER_PROPERTY_KEY to principal.name,
      PRISON_CODE_PROPERTY_KEY to appointment.prisonCode,
      APPOINTMENT_SET_ID_PROPERTY_KEY to appointment.id.toString(),
      CATEGORY_CODE_PROPERTY_KEY to appointment.categoryCode,
      HAS_DESCRIPTION_PROPERTY_KEY to (appointment.appointmentDescription != null).toString(),
      INTERNAL_LOCATION_ID_PROPERTY_KEY to appointment.internalLocationId.toString(),
      START_DATE_PROPERTY_KEY to appointment.startDate.toString(),
      EARLIEST_START_TIME_PROPERTY_KEY to appointment.appointments.minOf { it.startTime }.toString(),
      LATEST_END_TIME_PROPERTY_KEY to appointment.appointments.mapNotNull { it.endTime }.maxOf { it }.toString(),
    )

    val metricsMap = mapOf(
      APPOINTMENT_COUNT_METRIC_KEY to appointment.appointments.size.toDouble(),
      APPOINTMENT_INSTANCE_COUNT_METRIC_KEY to appointment.appointments.flatMap { it.occurrences.flatMap { occurrence -> occurrence.allocations } }.size.toDouble(),
      DESCRIPTION_LENGTH_METRIC_KEY to (appointment.appointmentDescription?.length ?: 0).toDouble(),
      EXTRA_INFORMATION_COUNT_METRIC_KEY to appointment.appointments.filterNot { it.comment.isNullOrEmpty() }.size.toDouble(),
      EVENT_TIME_MS_METRIC_KEY to (System.currentTimeMillis() - startTimeInMs).toDouble(),
    )

    telemetryClient.trackEvent(TelemetryEvent.APPOINTMENT_SET_CREATED.value, propertiesMap, metricsMap)
  }

  private fun writeAppointmentCreatedAuditRecord(request: AppointmentCreateRequest, appointment: Appointment) {
    auditService.logEvent(
      AppointmentCreatedEvent(
        appointmentId = appointment.id,
        prisonCode = appointment.prisonCode,
        categoryCode = appointment.categoryCode,
        hasDescription = appointment.appointmentDescription != null,
        internalLocationId = appointment.internalLocationId,
        startDate = appointment.startDate,
        startTime = appointment.startTime,
        endTime = appointment.endTime,
        isRepeat = request.repeat != null,
        repeatPeriod = request.repeat?.period,
        repeatCount = request.repeat?.count,
        hasExtraInformation = appointment.comment?.isNotEmpty() == true,
        prisonerNumbers = request.prisonerNumbers,
        createdAt = LocalDateTime.now(),
      ),
    )
  }
  private fun writeBulkAppointmentCreatedAuditRecord(request: BulkAppointmentsRequest, appointment: BulkAppointment) {
    auditService.logEvent(
      BulkAppointmentCreatedEvent(
        bulkAppointmentId = appointment.id,
        prisonCode = appointment.prisonCode,
        categoryCode = appointment.categoryCode,
        hasDescription = appointment.appointmentDescription != null,
        internalLocationId = appointment.internalLocationId,
        startDate = appointment.startDate,
        prisonerNumbers = request.appointments.map { it.prisonerNumber },
        createdAt = LocalDateTime.now(),
      ),
    )
  }
}
