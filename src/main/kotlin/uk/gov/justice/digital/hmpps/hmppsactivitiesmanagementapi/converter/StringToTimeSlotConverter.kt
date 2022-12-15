package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.converter

import org.springframework.core.convert.converter.Converter
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import java.util.Locale

class StringToTimeSlotConverter : Converter<String, TimeSlot> {
  override fun convert(value: String): TimeSlot = TimeSlot.valueOf(value.uppercase(Locale.getDefault()))
}
