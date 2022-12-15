package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common

class PayBand private constructor(val value: String) {
  companion object {
    fun valueOf(value: String) =
      if (value.isNotBlank())
        PayBand(
          value.trim().uppercase()
        ) else throw IllegalArgumentException("Pay band cannot be blank.")
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true

    other as PayBand

    return value == other.value
  }

  override fun hashCode(): Int = value.hashCode()

  override fun toString() = value
}

fun String.toPayBand() = PayBand.valueOf(this)
