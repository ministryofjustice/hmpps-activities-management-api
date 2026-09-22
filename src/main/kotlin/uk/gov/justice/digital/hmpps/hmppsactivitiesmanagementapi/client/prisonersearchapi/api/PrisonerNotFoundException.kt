package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api

class PrisonerNotFoundException(prisonerNumber: String) : RuntimeException("Prisoner not found: $prisonerNumber")
