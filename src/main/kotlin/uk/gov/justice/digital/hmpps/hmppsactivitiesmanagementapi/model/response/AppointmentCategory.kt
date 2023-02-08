package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
  description =
  """
  Describes an activity category. Categories can have a two level hierarchy, category and subcategory.
  Subcategory level categories will have a parent.
  Tables referencing appointment category should use the id primary key.
  Mapping to NOMIS is via the code property.
  The active property is a soft delete allowing categories that only exist in NOMIS to be maintained in the database
  but not supported when creating or editing appointments.
  Display order supports explicit ordering of categories and subcategories. Ordering will default to alphabetically
  by description of display order is not specified.
  """
)
data class AppointmentCategory(
  @Schema(
    description = "The internally generated identifier for this appointment category",
    example = "51"
  )
  val id: Long,

  @Schema(
    description = "The parent category. Signifies that this is a subcategory if not null"
  )
  val parent: AppointmentCategory? = null,

  @Schema(
    description = "The NOMIS REFERENCE_CODES.CODE (DOMAIN = 'INT_SCH_RSN') value for mapping to NOMIS",
    example = "CHAP"
  )
  val code: String,

  @Schema(
    description = "The description of the appointment category",
    example = "Chaplaincy"
  )
  val description: String,

  @Schema(
    description = "Flag to indicate if this (sub)category) is active. Only active (sub)categories are valid for create and update requests",
    example = "true"
  )
  val active: Boolean,

  @Schema(
    description = "Override to the default of ordering alphabetically by description supporting explicit ordering",
    example = "1"
  )
  val displayOrder: Int?
)
