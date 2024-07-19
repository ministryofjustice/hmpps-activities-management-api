package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.adjudications

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.adjudications.AdjudicationsHearingAdapter.Companion.mapOicHearingType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.rangeTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toIsoDateTime
import java.time.LocalDate
import java.time.LocalDateTime

class AdjudicationsHearingAdapterTest {

  private val manageAdjudicationsApiFacade: ManageAdjudicationsApiFacade = mock()
  private val adjudicationsHearingAdapter = AdjudicationsHearingAdapter(
    manageAdjudicationsApiFacade = manageAdjudicationsApiFacade,
  )

  val now: LocalDateTime = LocalDate.now().atStartOfDay().plusHours(4)

  @Nested
  inner class AdjudicationHearings {

    @BeforeEach
    fun init() {
      runBlocking {
        whenever(manageAdjudicationsApiFacade.getAdjudicationHearings(any(), any(), any(), any())).thenReturn(
          listOf(
            HearingsResponse(
              prisonerNumber = "AE12345",
              hearing = Hearing(
                id = 1,
                dateTimeOfHearing = now,
                oicHearingType = "INAD_ADULT",
                agencyId = "MDI",
                locationId = 1,
              ),
            ),
            HearingsResponse(
              prisonerNumber = "AE12345",
              hearing = Hearing(
                id = 2,
                dateTimeOfHearing = now.plusHours(10),
                oicHearingType = "INAD_ADULT",
                agencyId = "MDI",
                locationId = 1,
              ),
            ),
          ),
        )
      }
    }

    @CsvSource("true", "false")
    @ParameterizedTest
    fun `empty prisoners list skips call to facade`(mode: Boolean): Unit = runBlocking {
      val response = adjudicationsHearingAdapter.getAdjudicationHearings(
        agencyId = "MDI",
        dateRange = LocalDate.now().rangeTo(LocalDate.now()),
        prisonerNumbers = emptySet(),
      )
      assertThat(response.isEmpty()).isTrue()

      verify(manageAdjudicationsApiFacade, never()).getAdjudicationHearings(any(), any(), any(), any())
    }

    @Test
    fun `time slot filter `(): Unit = runBlocking {
      val hearings =
        adjudicationsHearingAdapter.getAdjudicationHearings(
          agencyId = "MDI",
          dateRange = LocalDate.now().rangeTo(LocalDate.now()),
          prisonerNumbers = setOf("AE12345"),
          timeSlot = TimeSlot.AM,
        )

      assertThat(hearings.size).isEqualTo(1)
      assertThat(hearings.first().hearingId).isEqualTo(1)
    }

    @Test
    fun `fields mapped correctly`(): Unit = runBlocking {
      val hearings = adjudicationsHearingAdapter.getAdjudicationHearings(
        agencyId = "MDI",
        dateRange = LocalDate.now().rangeTo(LocalDate.now()),
        prisonerNumbers = setOf("AE12345"),
      )

      assertThat(hearings.first().offenderNo).isEqualTo("AE12345")
      assertThat(hearings.first().startTime).isEqualTo(now.toIsoDateTime())
      assertThat(hearings.first().internalLocationId).isEqualTo(1)
      assertThat(hearings.first().agencyId).isEqualTo("MDI")
    }

    @CsvSource("INAD_GOV", "INAD_YOI", "GOV_ADULT", "GOV_YOI", "Random")
    @ParameterizedTest
    fun `hearing type mappings`(oicHearingType: String): Unit = runBlocking {
      assertThat(oicHearingType.mapOicHearingType()).isEqualTo(
        when (oicHearingType) {
          "GOV_ADULT" -> "Governor's Hearing Adult"
          "GOV_YOI" -> "Governor's Hearing YOI"
          "INAD_ADULT" -> "Independent Adjudicator Hearing Adult"
          "INAD_YOI" -> "Independent Adjudicator Hearing YOI"
          else -> "Governor's Hearing Adult"
        },
      )
    }
  }

  @Nested
  inner class AjudicationHearingsByLocation {

    @BeforeEach
    fun init() {
      runBlocking {
        whenever(manageAdjudicationsApiFacade.getAdjudicationHearingsForDate(any(), any())).thenReturn(
          HearingSummaryResponse(
            hearings = listOf(
              HearingSummaryDto(
                id = 1,
                dateTimeOfHearing = now,
                oicHearingType = "INAD_ADULT",
                locationId = 1,
                prisonerNumber = "AE12345",
                status = "",
                chargeNumber = "",
                dateTimeOfDiscovery = LocalDateTime.now(),
              ),
              HearingSummaryDto(
                id = 2,
                dateTimeOfHearing = now.plusHours(10),
                oicHearingType = "INAD_ADULT",
                locationId = 1,
                prisonerNumber = "AE12345",
                status = "",
                chargeNumber = "",
                dateTimeOfDiscovery = LocalDateTime.now(),
              ),
            ),
          ),
        )
      }
    }

    @Test
    fun `test mappings`(): Unit = runBlocking {
      val hearings =
        adjudicationsHearingAdapter.getAdjudicationsByLocation(
          agencyId = "MDI",
          date = LocalDate.now(),
          timeSlot = null,
        )

      assertThat(hearings.size).isEqualTo(1)
      assertThat(hearings[1]!!.size).isEqualTo(2)
      assertThat(hearings[1]!!.first().hearingId).isEqualTo(1)
      assertThat(hearings[1]!!.first().offenderNo).isEqualTo("AE12345")
      assertThat(hearings[1]!!.first().startTime).isEqualTo(now.toIsoDateTime())
      assertThat(hearings[1]!!.first().internalLocationId).isEqualTo(1)
      assertThat(hearings[1]!!.first().agencyId).isEqualTo("MDI")
    }

    @Test
    fun `time slot filter`(): Unit = runBlocking {
      val hearings =
        adjudicationsHearingAdapter.getAdjudicationsByLocation(
          agencyId = "MDI",
          date = LocalDate.now(),
          timeSlot = TimeSlot.AM,
        )

      assertThat(hearings.size).isEqualTo(1)
      assertThat(hearings[1]!!.first().hearingId).isEqualTo(1)
    }
  }
}
