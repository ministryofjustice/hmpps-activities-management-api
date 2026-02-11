package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import io.mockk.MockKAnnotations
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PurposefulActivityRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class PurposefulActivityServiceTest {
  private val paRepository: PurposefulActivityRepository = mockk(relaxed = true)
  private val s3Service: S3Service = mockk(relaxed = true)

  val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
  private val paService = PurposefulActivityService(paRepository, s3Service)

  private val mockDate = LocalDateTime.now()

  @BeforeEach
  fun setUp() {
    MockKAnnotations.init(this)
  }

  @Test
  fun `test convertToCsv with valid input`() {
    val reportData = mutableListOf(
      arrayOf<Any?>("John", mockDate, 123L, 456),
      arrayOf<Any?>("Jane", mockDate, 789L, 101112),
      arrayOf<Any?>("Alice", mockDate, null, 131415),
      arrayOf<Any?>(null, mockDate, 161718L, 192021),
    ).stream()

    val expectedCsv = """
            "John","${dateFormat.format(mockDate)}",123,456
            "Jane","${dateFormat.format(mockDate)}",789,101112
            "Alice","${dateFormat.format(mockDate)}",,131415
            ,"${dateFormat.format(mockDate)}",161718,192021
    """.trimIndent()

    val csvOutput = paService.getResultsAsCsv(reportData)

    val actualCsv = csvOutput.readText()

    assertThat(actualCsv).isEqualTo(expectedCsv)
  }
}
