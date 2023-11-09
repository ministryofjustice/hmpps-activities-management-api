package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentSeriesCreatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentCancellationReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentSeriesRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.CANCELLED_APPOINTMENT_CANCELLATION_REASON_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AuditService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.TransactionHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_INSTANCE_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_SERIES_ID_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.CATEGORY_CODE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.CATEGORY_DESCRIPTION_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.CUSTOM_NAME_LENGTH_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.END_TIME_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EVENT_TIME_MS_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EXTRA_INFORMATION_LENGTH_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.FREQUENCY_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.HAS_CUSTOM_NAME_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.HAS_EXTRA_INFORMATION_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.INTERNAL_LOCATION_DESCRIPTION_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.INTERNAL_LOCATION_ID_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.IS_REPEAT_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.NUMBER_OF_APPOINTMENTS_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISONER_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISON_CODE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.START_DATE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.START_TIME_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.USER_PROPERTY_KEY
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeries as AppointmentSeriesModel

@Service
@Transactional
class AppointmentCreateDomainService(
  private val appointmentSeriesRepository: AppointmentSeriesRepository,
  private val appointmentRepository: AppointmentRepository,
  private val appointmentCancellationReasonRepository: AppointmentCancellationReasonRepository,
  private val transactionHandler: TransactionHandler,
  private val telemetryClient: TelemetryClient,
  private val auditService: AuditService,
) {
  fun createAppointments(
    appointmentSeriesId: Long,
    prisonNumberBookingIdMap: Map<String, Long>,
    startTimeInMs: Long,
    categoryDescription: String,
    locationDescription: String,
  ): AppointmentSeriesModel {
    val appointmentSeries = appointmentSeriesRepository.findOrThrowNotFound(appointmentSeriesId)
    return createAppointments(
      appointmentSeries = appointmentSeries,
      prisonNumberBookingIdMap = prisonNumberBookingIdMap,
      createFirstAppointmentOnly = false,
      isCancelled = false,
      categoryDescription = categoryDescription,
      locationDescription = locationDescription,
      startTimeInMs = startTimeInMs,
      trackEvent = true,
      auditEvent = true,
    )
  }

  /**
   * Uses the appointment series as a blueprint to create all the appointments in the series and their attendees.
   * Will only create appointments and attendees not already created making it safe to use for the partial async create process.
   *
   * @param isCancelled specifies whether the appointments should be created in a cancelled state. Can only set to true for migrated appointments
   */
  fun createAppointments(
    appointmentSeries: AppointmentSeries,
    prisonNumberBookingIdMap: Map<String, Long>,
    createFirstAppointmentOnly: Boolean = false,
    isCancelled: Boolean = false,
    startTimeInMs: Long = 0,
    categoryDescription: String = "",
    locationDescription: String = "",
    trackEvent: Boolean = false,
    auditEvent: Boolean = false,
  ): AppointmentSeriesModel {
    require(!isCancelled || appointmentSeries.isMigrated) {
      "Only migrated appointments can be created in a cancelled state"
    }

    val cancelledTime = if (isCancelled) appointmentSeries.updatedTime ?: appointmentSeries.createdTime else null
    val cancellationReason = if (isCancelled) appointmentCancellationReasonRepository.findOrThrowNotFound(CANCELLED_APPOINTMENT_CANCELLATION_REASON_ID) else null
    val cancelledBy = if (isCancelled) appointmentSeries.updatedBy ?: appointmentSeries.createdBy else null

    appointmentSeries.scheduleIterator().withIndex().forEach { indexedStartDate ->
      val sequenceNumber = indexedStartDate.index + 1

      if (createFirstAppointmentOnly && sequenceNumber > 1) return@forEach

      if (appointmentSeries.appointments().none { it.sequenceNumber == sequenceNumber }) {
        transactionHandler.newSpringTransaction {
          appointmentRepository.saveAndFlush(
            appointmentSeries.createAndAddAppointment(
              sequenceNumber,
              indexedStartDate.value,
            ).apply {
              this.cancelledTime = cancelledTime
              this.cancellationReason = cancellationReason
              this.cancelledBy = cancelledBy

              prisonNumberBookingIdMap.forEach {
                addAttendee(
                  AppointmentAttendee(
                    appointment = this,
                    prisonerNumber = it.key,
                    bookingId = it.value,
                  ),
                )
              }
            },
          )
        }

        appointmentSeriesRepository.saveAndFlush(appointmentSeries)

        // TODO: publish appointment instance created message post transaction
      }
    }

    return appointmentSeries.toModel().also {
      if (trackEvent) it.logAppointmentSeriesCreatedMetric(prisonNumberBookingIdMap, startTimeInMs, categoryDescription, locationDescription)
      if (auditEvent) it.writeAppointmentCreatedAuditRecord(prisonNumberBookingIdMap)
    }
  }

  private fun AppointmentSeriesModel.logAppointmentSeriesCreatedMetric(
    prisonNumberBookingIdMap: Map<String, Long>,
    startTimeInMs: Long,
    categoryDescription: String,
    locationDescription: String,
  ) {
    val propertiesMap = mapOf(
      USER_PROPERTY_KEY to createdBy,
      PRISON_CODE_PROPERTY_KEY to prisonCode,
      APPOINTMENT_SERIES_ID_PROPERTY_KEY to id.toString(),
      CATEGORY_CODE_PROPERTY_KEY to categoryCode,
      CATEGORY_DESCRIPTION_PROPERTY_KEY to categoryDescription,
      HAS_CUSTOM_NAME_PROPERTY_KEY to (!customName.isNullOrEmpty()).toString(),
      INTERNAL_LOCATION_ID_PROPERTY_KEY to (if (this.inCell) "" else this.internalLocationId?.toString() ?: ""),
      INTERNAL_LOCATION_DESCRIPTION_PROPERTY_KEY to locationDescription,
      START_DATE_PROPERTY_KEY to startDate.toString(),
      START_TIME_PROPERTY_KEY to startTime.toString(),
      END_TIME_PROPERTY_KEY to endTime.toString(),
      IS_REPEAT_PROPERTY_KEY to (schedule != null).toString(),
      FREQUENCY_PROPERTY_KEY to (schedule?.frequency?.toString() ?: ""),
      NUMBER_OF_APPOINTMENTS_PROPERTY_KEY to (schedule?.numberOfAppointments?.toString() ?: ""),
      HAS_EXTRA_INFORMATION_PROPERTY_KEY to (extraInformation?.isNotEmpty() == true).toString(),
    )

    val metricsMap = mapOf(
      PRISONER_COUNT_METRIC_KEY to prisonNumberBookingIdMap.size.toDouble(),
      APPOINTMENT_COUNT_METRIC_KEY to (schedule?.numberOfAppointments ?: 1).toDouble(),
      APPOINTMENT_INSTANCE_COUNT_METRIC_KEY to (prisonNumberBookingIdMap.size * (schedule?.numberOfAppointments ?: 1)).toDouble(),
      CUSTOM_NAME_LENGTH_METRIC_KEY to (customName?.length ?: 0).toDouble(),
      EXTRA_INFORMATION_LENGTH_METRIC_KEY to (extraInformation?.length ?: 0).toDouble(),
      EVENT_TIME_MS_METRIC_KEY to (System.currentTimeMillis() - startTimeInMs).toDouble(),
    )

    telemetryClient.trackEvent(TelemetryEvent.APPOINTMENT_SERIES_CREATED.value, propertiesMap, metricsMap)
  }

  private fun AppointmentSeriesModel.writeAppointmentCreatedAuditRecord(prisonNumberBookingIdMap: Map<String, Long>) {
    auditService.logEvent(
      AppointmentSeriesCreatedEvent(
        appointmentSeriesId = id,
        prisonCode = prisonCode,
        categoryCode = categoryCode,
        hasCustomName = customName != null,
        internalLocationId = internalLocationId,
        startDate = startDate,
        startTime = startTime,
        endTime = endTime,
        isRepeat = schedule != null,
        frequency = schedule?.frequency,
        numberOfAppointments = schedule?.numberOfAppointments,
        hasExtraInformation = extraInformation?.isNotEmpty() == true,
        prisonerNumbers = prisonNumberBookingIdMap.keys.toList(),
        createdTime = createdTime,
        createdBy = createdBy,
      ),
    )
  }
}

/**
 * Creates an appointment within an appointment series based on the series blueprint.
 * This function uses the parent appointment series' updatedTime and updatedBy values even though it's creating the
 * initial appointment data as migrated appointments can have these values set. When we migrate an appointment, we are
 * becoming the master record for that appointment so want to bring all the data we can over. When that appointment was
 * last updated and by whom is part of that data.
 */
fun AppointmentSeries.createAndAddAppointment(sequenceNumber: Int, startDate: LocalDate) =
  Appointment(
    appointmentSeries = this,
    sequenceNumber = sequenceNumber,
    prisonCode = this.prisonCode,
    categoryCode = this.categoryCode,
    customName = this.customName,
    appointmentTier = this.appointmentTier,
    appointmentHost = this.appointmentHost,
    internalLocationId = this.internalLocationId,
    customLocation = this.customLocation,
    inCell = this.inCell,
    onWing = this.onWing,
    offWing = this.offWing,
    startDate = startDate,
    startTime = this.startTime,
    endTime = this.endTime,
    unlockNotes = this.unlockNotes,
    extraInformation = this.extraInformation,
    createdTime = this.createdTime,
    createdBy = this.createdBy,
    updatedTime = this.updatedTime,
    updatedBy = this.updatedBy,
  ).apply {
    addAppointment(this)
  }
