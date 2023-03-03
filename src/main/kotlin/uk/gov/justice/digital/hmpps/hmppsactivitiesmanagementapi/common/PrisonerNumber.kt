package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common

class PrisonerNumber private constructor(val value: String) {
  companion object {
    fun valueOf(value: String) =
      if (value.isNotBlank()) {
        PrisonerNumber(
          value.trim().uppercase(),
        )
      } else {
        throw IllegalArgumentException("Prisoner number cannot be blank.")
      }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true

    other as PrisonerNumber

    return value == other.value
  }

  override fun hashCode(): Int = value.hashCode()

  override fun toString() = value
}

fun String.toPrisonerNumber() = PrisonerNumber.valueOf(this)
