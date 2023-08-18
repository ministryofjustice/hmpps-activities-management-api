package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentRepeatPeriod
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.CreateAppointmentOccurrencesJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentRepeat
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentMigrateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.BulkAppointmentsRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentCancellationReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.BulkAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.CANCELLED_APPOINTMENT_CANCELLATION_REASON_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.checkCaseloadAccess
import java.security.Principal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Appointment as AppointmentEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrence as AppointmentOccurrenceEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrenceAllocation as AppointmentOccurrenceAllocationEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.BulkAppointment as BulkAppointmentEntity

@Service
class AppointmentService(
  private val appointmentRepository: AppointmentRepository,
  private val appointmentCancellationReasonRepository: AppointmentCancellationReasonRepository,
  private val bulkAppointmentRepository: BulkAppointmentRepository,
  private val referenceCodeService: ReferenceCodeService,
  private val locationService: LocationService,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val createAppointmentOccurrencesJob: CreateAppointmentOccurrencesJob,
  @Value("\${applications.max-appointment-instances}") private val maxAppointmentInstances: Int = 20000,
  @Value("\${applications.max-sync-appointment-instance-actions}") private val maxSyncAppointmentInstanceActions: Int = 500,
) {
  @Transactional(readOnly = true)
  fun getAppointmentById(appointmentId: Long): Appointment {
    val appointment = appointmentRepository.findOrThrowNotFound(appointmentId).toModel()
    checkCaseloadAccess(appointment.prisonCode)

    return appointment
  }

  fun bulkCreateAppointments(request: BulkAppointmentsRequest, principal: Principal) =
    require(request.appointments.isNotEmpty()) { "One or more appointments must be supplied." }.run {
      createPrisonerMap(request.appointments.map { it.prisonerNumber }, request.prisonCode).let { prisonerBookings ->
        BulkAppointmentEntity(
          prisonCode = request.prisonCode,
          categoryCode = request.categoryCode,
          appointmentDescription = request.appointmentDescription,
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
              internalLocationId = request.internalLocationId,
              inCell = request.inCell,
              startDate = request.startDate,
              startTime = it.startTime,
              endTime = it.endTime,
              comment = it.comment,
              createdBy = principal.name,
            )
          }.forEach { appointment -> this.addAppointment(appointment) }
        }.let { bulkAppointmentRepository.saveAndFlush(it).toModel() }
      }
    }

  fun createAppointment(request: AppointmentCreateRequest, principal: Principal): Appointment {
    val prisonerBookings = createPrisonerMap(request.prisonerNumbers, request.prisonCode)
    // Determine if this is a create request for a very large appointment. If it is, this function will only create the first occurrence
    val createFirstOccurrenceOnly = request.repeat?.count?.let { it > 1 && it * prisonerBookings.size > maxSyncAppointmentInstanceActions } ?: false

    val appointment = appointmentRepository.saveAndFlush(
      buildValidAppointmentEntity(
        appointmentType = request.appointmentType,
        prisonCode = request.prisonCode!!,
        prisonerNumbers = request.prisonerNumbers,
        prisonerBookings = prisonerBookings,
        inCell = request.inCell,
        categoryCode = request.categoryCode,
        appointmentDescription = request.appointmentDescription,
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
      createAppointmentOccurrencesJob.execute(appointment.appointmentId, prisonerBookings)
    }

    return appointment.toModel()
  }

  fun migrateAppointment(request: AppointmentMigrateRequest, principal: Principal) =
    mapOf(request.prisonerNumber!! to request.bookingId.toString()).let { prisonerBookings ->
      buildValidAppointmentEntity(
        appointmentType = AppointmentType.INDIVIDUAL,
        prisonCode = request.prisonCode!!,
        prisonerNumbers = listOf(request.prisonerNumber),
        prisonerBookings = prisonerBookings,
        categoryCode = request.categoryCode,
        internalLocationId = request.internalLocationId,
        inCell = false,
        startDate = request.startDate,
        startTime = request.startTime,
        endTime = request.endTime,
        comment = request.comment ?: "",
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
    categoryCode: String? = null,
    appointmentDescription: String? = null,
    internalLocationId: Long? = null,
    inCell: Boolean = false,
    startDate: LocalDate?,
    startTime: LocalTime?,
    endTime: LocalTime?,
    repeat: AppointmentRepeat? = null,
    comment: String = "",
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
      failIfCategoryNotFound(categoryCode!!)
      failIfLocationNotFound(inCell, prisonCode, internalLocationId)
      failIfMissingPrisoners(prisonerNumbers, prisonerBookings)
    }

    return AppointmentEntity(
      appointmentType = appointmentType!!,
      prisonCode = prisonCode,
      categoryCode = categoryCode ?: "",
      appointmentDescription = appointmentDescription?.takeUnless(String::isBlank),
      internalLocationId = if (inCell) null else internalLocationId,
      inCell = inCell,
      startDate = startDate!!,
      startTime = startTime!!,
      endTime = endTime,
      comment = comment,
      created = created,
      createdBy = createdBy,
      updated = updated,
      updatedBy = updatedBy,
      isMigrated = isMigrated,
    ).apply {
      this.schedule = repeat?.let {
        AppointmentSchedule(
          appointment = this,
          repeatPeriod = AppointmentRepeatPeriod.valueOf(repeat.period!!.name),
          repeatCount = repeat.count!!,
        )
      }

      this.scheduleIterator().withIndex().forEach {
        if (createFirstOccurrenceOnly && it.index > 0) return@forEach

        this.addOccurrence(
          AppointmentOccurrenceEntity(
            appointment = this,
            sequenceNumber = it.index + 1,
            internalLocationId = this.internalLocationId,
            inCell = this.inCell,
            startDate = it.value,
            startTime = this.startTime,
            endTime = this.endTime,
            updated = updated,
            updatedBy = updatedBy,
          ).apply {
            prisonerBookings.forEach { prisonerBooking ->
              this.addAllocation(
                AppointmentOccurrenceAllocationEntity(
                  appointmentOccurrence = this,
                  prisonerNumber = prisonerBooking.key,
                  bookingId = prisonerBooking.value!!.toLong(),
                ),
              )
            }
            if (isCancelled) {
              cancelled = updated ?: created
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
}
