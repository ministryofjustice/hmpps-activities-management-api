package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.PrisonPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonPayBand as PrisonPayBandModel

class ActivityPayTest {
  @Test
  fun `entity is converted to model`() {
    val activity = activityEntity()
    val activityPayEntity = ActivityPay(
      1,
      activity,
      incentiveNomisCode = "STD",
      incentiveLevel = "Standard",
      payBand = PrisonPayBand(
        prisonPayBandId = 1,
        displaySequence = 1,
        nomisPayBand = 1,
        payBandAlias = "Low",
        payBandDescription = "Pay band 1",
        prisonCode = "MDI",
      ),
      rate = 100,
      pieceRate = 150,
      pieceRateItems = 1,
    )

    with(activityPayEntity.toModel()) {
      assertThat(id).isEqualTo(1)
      assertThat(incentiveNomisCode).isEqualTo("STD")
      assertThat(incentiveLevel).isEqualTo("Standard")
      assertThat(prisonPayBand).isEqualTo(
        PrisonPayBandModel(
          id = 1,
          displaySequence = 1,
          nomisPayBand = 1,
          alias = "Low",
          description = "Pay band 1",
          prisonCode = "MDI",
        ),
      )
      assertThat(rate).isEqualTo(100)
      assertThat(pieceRate).isEqualTo(150)
      assertThat(pieceRateItems).isEqualTo(1)
    }
  }

  @Test
  fun `entity is converted to model lite`() {
    val activity = activityEntity()
    val activityPayEntity = ActivityPay(
      1,
      activity,
      incentiveNomisCode = "STD",
      incentiveLevel = "Standard",
      payBand = PrisonPayBand(
        prisonPayBandId = 1,
        displaySequence = 1,
        nomisPayBand = 1,
        payBandAlias = "Low",
        payBandDescription = "Pay band 1",
        prisonCode = "MDI",
      ),
      rate = 100,
      pieceRate = 150,
      pieceRateItems = 1,
    )

    with(activityPayEntity.toModelLite()) {
      assertThat(id).isEqualTo(1)
      assertThat(incentiveNomisCode).isEqualTo("STD")
      assertThat(incentiveLevel).isEqualTo("Standard")
      assertThat(prisonPayBandId).isEqualTo(1)
      assertThat(rate).isEqualTo(100)
      assertThat(pieceRate).isEqualTo(150)
      assertThat(pieceRateItems).isEqualTo(1)
    }
  }
}
