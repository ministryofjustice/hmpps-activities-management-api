package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import io.mockk.MockKAnnotations
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PurposefulActivityRepository
import java.text.SimpleDateFormat
import java.util.*

class PurposefulActivityServiceTest {
  private val paRepository: PurposefulActivityRepository = mockk(relaxed = true)
  private val s3Service: S3Service = mockk(relaxed = true)

  private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  private val paService = PurposefulActivityService(paRepository, s3Service)

  private val mockDate = Date(1726819200000)

  @BeforeEach
  fun setUp() {
    MockKAnnotations.init(this)
  }

  @Test
  fun `test convertToCsv with valid input`() {
    val reportData = mutableListOf(
      arrayOf("John", mockDate, 123L, 456),
      arrayOf("Jane", mockDate, 789L, 101112),
      arrayOf("Alice", mockDate, null, 131415),
      arrayOf(null, mockDate, 161718L, 192021),
    ).stream()

    val expectedCsv = """
            "John","${dateFormat.format(mockDate)}",123,456
            "Jane","${dateFormat.format(mockDate)}",789,101112
            "Alice","${dateFormat.format(mockDate)}",,131415
            ,"${dateFormat.format(mockDate)}",161718,192021
    """.trimIndent()

    val csvOutput = paService.getResultsAsCsv(reportData.stream())

    val actualCsv = csvOutput.readText()

    assertThat(actualCsv).isEqualTo(expectedCsv)
  }
}
