package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.io.StringReader
import java.util.UUID

class LocationRemapFileTest {
  private val reader = LocationRemapFile()
  private val source = UUID.fromString("11111111-1111-1111-1111-111111111111")
  private val target = UUID.fromString("22222222-2222-2222-2222-222222222222")
  private val header = "prisonId,locationType,locationId,chosenChildId,locationName\r\n"

  @Test
  fun `reads quoted fields CRLF BOM and duplicate rows`() {
    val row = "GMI,PARENT,$source,$target,\"Laundry, main\"\r\n"
    assertThat(reader.read(StringReader("\uFEFF$header$row$row"))).containsExactly(LocationRemap("GMI", source, target, "Laundry, main"))
  }

  @Test
  fun `rejects conflicting mappings`() {
    assertThatThrownBy {
      reader.read(StringReader(header + "GMI,PARENT,$source,$target,Laundry\r\nGMI,PARENT,$source,33333333-3333-3333-3333-333333333333,Laundry\r\n"))
    }.hasMessageContaining("Conflicting")
  }

  @Test
  fun `rejects missing headers self maps invalid UUIDs and empty files`() {
    listOf("prisonId\r\nGMI\r\n", header, header + "GMI,PARENT,$source,$source,Laundry\r\n", header + "GMI,PARENT,invalid,$target,Laundry\r\n").forEach { csv ->
      assertThatThrownBy { reader.read(StringReader(csv)) }.isInstanceOf(IllegalArgumentException::class.java)
    }
  }

  @Test
  fun `loads supplied preprod mapping including all pilot prisons`() {
    val mappings = reader.load("MAPB-924-preprod-mapping-2026-10-16.csv")
    assertThat(mappings).isNotEmpty
    assertThat(mappings.map { it.prisonId }).containsAll(LocationRemapService.PILOT_PRISONS)
  }

  @Test
  fun `rejects paths outside bundled mappings`() {
    assertThatThrownBy { reader.load("../other.csv") }.hasMessageContaining("basename")
  }
}
