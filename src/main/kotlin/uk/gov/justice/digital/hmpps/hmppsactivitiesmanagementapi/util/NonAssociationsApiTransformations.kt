package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nonassociationsapi.model.NonAssociation

/**
 * Transform functions providing a thin layer to transform non-associations api types into their API model equivalents and vice-versa.
 */

fun List<NonAssociation>.hasNonAssociations(prisonerNumber: String) =
  this.any { nonAssociation -> nonAssociation.firstPrisonerNumber == prisonerNumber || nonAssociation.secondPrisonerNumber == prisonerNumber }
