package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nonassociationsapi.api.extensions

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nonassociationsapi.model.PrisonerNonAssociation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.nonassociation.NonAssociationDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.nonassociation.OtherPrisonerDetails
import java.time.LocalDateTime

fun PrisonerNonAssociation.toModel(isAllocated: Boolean = true) = NonAssociationDetails(
  allocated = isAllocated,
  reasonCode = this.reason.toString(),
  reasonDescription = this.reasonDescription,
  roleCode = this.role.toString(),
  roleDescription = this.roleDescription,
  restrictionType = this.restrictionType.toString(),
  restrictionTypeDescription = this.restrictionTypeDescription,
  otherPrisonerDetails = with(this.otherPrisonerDetails) {
    OtherPrisonerDetails(
      prisonerNumber = this.prisonerNumber,
      firstName = this.firstName,
      lastName = this.lastName,
      cellLocation = this.cellLocation,
    )
  },
  whenUpdated = LocalDateTime.parse(this.whenUpdated),
  comments = this.comment,
)
