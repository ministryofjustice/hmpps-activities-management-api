package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentCreateDomainService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentFrequency
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.CreateAppointmentsJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeries
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeriesDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentSeriesCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentHostRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentSeriesRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentTierRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.NOT_SPECIFIED_APPOINTMENT_TIER_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.checkCaseloadAccess
import java.security.Principal
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSeries as AppointmentSeriesEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSeriesSchedule as AppointmentSeriesScheduleEntity

@Service
@Transactional
class AppointmentSeriesService(
  private val appointmentSeriesRepository: AppointmentSeriesRepository,
  private val appointmentTierRepository: AppointmentTierRepository,
  private val appointmentHostRepository: AppointmentHostRepository,
  private val referenceCodeService: ReferenceCodeService,
  private val locationService: LocationService,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val prisonApiClient: PrisonApiClient,
  private val appointmentCreateDomainService: AppointmentCreateDomainService,
  private val createAppointmentsJob: CreateAppointmentsJob,
  private val transactionHandler: TransactionHandler,
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
    val startTimeInMs = System.currentTimeMillis()

    checkCaseloadAccess(request.prisonCode!!)

    request.failIfMaximumAppointmentInstancesExceeded()
    val categoryDescription = request.categoryDescription()
    val locationDescription = request.locationDescription()

    val prisonNumberBookingIdMap = request.createNumberBookingIdMap()
    request.failIfMissingPrisoners(prisonNumberBookingIdMap)

    val appointmentTier = appointmentTierRepository.findOrThrowNotFound(NOT_SPECIFIED_APPOINTMENT_TIER_ID)

    // Determine if this is a create request for a very large appointment series. If it is, this function will only create the first appointment
    val createFirstAppointmentOnly = request.schedule?.numberOfAppointments?.let { it > 1 && it * prisonNumberBookingIdMap.size > maxSyncAppointmentInstanceActions } ?: false

    val appointmentSeriesModel =
      transactionHandler.newSpringTransaction {
        appointmentSeriesRepository.saveAndFlush(request.toAppointmentSeries(appointmentTier, principal.name))
      }.let {
        appointmentCreateDomainService.createAppointments(
          appointmentSeries = it,
          prisonNumberBookingIdMap = prisonNumberBookingIdMap,
          createFirstAppointmentOnly = createFirstAppointmentOnly,
          isCancelled = false,
          startTimeInMs = startTimeInMs,
          categoryDescription = categoryDescription,
          locationDescription = locationDescription,
          trackEvent = !createFirstAppointmentOnly,
          auditEvent = !createFirstAppointmentOnly,
        )
      }

    if (createFirstAppointmentOnly) {
      // The remaining appointments will be created asynchronously by this job
      createAppointmentsJob.execute(
        appointmentSeriesId = appointmentSeriesModel.id,
        prisonNumberBookingIdMap = prisonNumberBookingIdMap,
        startTimeInMs = startTimeInMs,
        categoryDescription = categoryDescription,
        locationDescription = locationDescription,
      )
    }

    return appointmentSeriesModel
  }

  private fun AppointmentSeriesCreateRequest.failIfMaximumAppointmentInstancesExceeded() {
    val repeatCount = schedule?.numberOfAppointments ?: 1
    require(prisonerNumbers.size * repeatCount <= maxAppointmentInstances) {
      "You cannot schedule more than ${maxAppointmentInstances / prisonerNumbers.size} appointments for this number of attendees."
    }
  }

  private fun AppointmentSeriesCreateRequest.categoryDescription() =
    referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT)[categoryCode]?.description
      ?: throw IllegalArgumentException("Appointment Category with code '$categoryCode' not found or is not active")

  private fun AppointmentSeriesCreateRequest.locationDescription(): String {
    return if (inCell) {
      "In cell"
    } else {
      locationService.getLocationsForAppointmentsMap(prisonCode!!)[internalLocationId]?.let { it.userDescription ?: it.description }
        ?: throw IllegalArgumentException("Appointment location with id '$internalLocationId' not found in prison '$prisonCode'")
    }
  }

  private fun AppointmentSeriesCreateRequest.createNumberBookingIdMap() =
    prisonerSearchApiClient.findByPrisonerNumbers(prisonerNumbers)
      .filter { prisoner -> prisoner.prisonId == prisonCode }
      .associate { it.prisonerNumber to it.bookingId!!.toLong() }

  private fun AppointmentSeriesCreateRequest.failIfMissingPrisoners(prisonNumberBookingIdMap: Map<String, Long>) {
    prisonerNumbers.filterNot(prisonNumberBookingIdMap::containsKey).let {
      require(it.isEmpty()) {
        "Prisoner(s) with prisoner number(s) '${it.joinToString("', '")}' not found, were inactive or are residents of a different prison."
      }
    }
  }

  private fun AppointmentSeriesCreateRequest.toAppointmentSeries(appointmentTier: AppointmentTier, createdBy: String) =
    AppointmentSeriesEntity(
      appointmentType = appointmentType!!,
      prisonCode = prisonCode!!,
      categoryCode = categoryCode!!,
      customName = customName?.takeUnless(String::isBlank),
      appointmentTier = appointmentTier,
      internalLocationId = if (inCell) null else internalLocationId,
      inCell = inCell,
      startDate = startDate!!,
      startTime = startTime!!,
      endTime = endTime,
      extraInformation = extraInformation?.trim()?.takeUnless(String::isBlank),
      createdBy = createdBy,
    ).also { appointmentSeries ->
      appointmentSeries.schedule = schedule?.let {
        AppointmentSeriesScheduleEntity(
          appointmentSeries = appointmentSeries,
          frequency = AppointmentFrequency.valueOf(it.frequency!!.name),
          numberOfAppointments = it.numberOfAppointments!!,
        )
      }
    }
}
