package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.apache.commons.csv.CSVFormat
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import java.io.Reader
import java.util.UUID

data class LocationRemap(val prisonId: String, val locationId: UUID, val chosenChildId: UUID, val locationName: String? = null)

@Component
class LocationRemapFile {
  fun load(file: String): List<LocationRemap> {
    require(file.matches(Regex("[A-Za-z0-9][A-Za-z0-9._-]*\\.csv"))) { "A bundled CSV basename is required" }
    return ClassPathResource("location-mappings/$file").inputStream.bufferedReader().use(::read)
  }

  fun read(reader: Reader): List<LocationRemap> {
    val buffered = reader.buffered()
    buffered.mark(1)
    if (buffered.read() != 0xFEFF) buffered.reset()
    return CSVFormat.RFC4180.builder().setHeader().setSkipHeaderRecord(true).setIgnoreEmptyLines(true).get().parse(buffered).use { parser ->
      require(parser.headerNames.containsAll(listOf("prisonId", "locationType", "locationId", "chosenChildId"))) { "Missing location mapping headers" }
      val mappings = parser.map { record ->
        require(record.isConsistent) { "Inconsistent CSV record ${record.recordNumber}" }
        require(record["prisonId"].matches(Regex("[A-Z]{3}"))) { "Invalid prison at record ${record.recordNumber}" }
        require(record["locationType"] in setOf("PARENT", "OTHER_CHILD")) { "Invalid location type at record ${record.recordNumber}" }
        LocationRemap(
          record["prisonId"],
          UUID.fromString(record["locationId"]),
          UUID.fromString(record["chosenChildId"]),
          if (record.isMapped("locationName")) record["locationName"].takeIf(String::isNotBlank) else null,
        )
      }.distinctBy { Triple(it.prisonId, it.locationId, it.chosenChildId) }
      require(mappings.groupBy { it.locationId }.values.all { it.size == 1 }) { "Conflicting source location mappings" }
      val sources = mappings.map { it.locationId }.toSet()
      require(mappings.none { it.chosenChildId in sources }) { "Self-referencing or chained location mappings" }
      require(mappings.isNotEmpty()) { "Location mapping file is empty" }
      mappings
    }
  }
}
