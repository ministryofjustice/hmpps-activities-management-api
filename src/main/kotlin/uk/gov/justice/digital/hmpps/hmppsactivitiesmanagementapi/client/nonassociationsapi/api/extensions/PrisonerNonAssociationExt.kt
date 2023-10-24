package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nonassociationsapi.api.extensions

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nonassociationsapi.model.PrisonerNonAssociation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.nonassociation.NonAssociationDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.nonassociation.OtherPrisonerDetails
import java.time.LocalDateTime

fun PrisonerNonAssociation.toModel() = NonAssociationDetails(
  reasonCode = this.reason.toString(),
  reasonDescription = this.reasonDescription,
  otherPrisonerDetails = with(this.otherPrisonerDetails) {
    OtherPrisonerDetails(
      prisonerNumber = this.prisonerNumber,
      firstName = this.firstName,
      lastName = this.lastName,
      cellLocation = this.cellLocation,
    )
  },
  whenCreated = LocalDateTime.parse(this.whenCreated),
  comments = this.comment,
)
