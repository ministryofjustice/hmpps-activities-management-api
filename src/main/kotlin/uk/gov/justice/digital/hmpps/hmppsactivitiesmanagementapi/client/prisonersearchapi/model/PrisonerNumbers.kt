package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import javax.validation.constraints.*
import javax.validation.Valid
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param prisonerNumbers List of prisoner numbers to search by
 */
data class PrisonerNumbers(

    @get:Size(min=1,max=1000) 
    @Schema(example = "[\"A1234AA\"]", required = true, description = "List of prisoner numbers to search by")
    @get:JsonProperty("prisonerNumbers", required = true) val prisonerNumbers: kotlin.collections.List<kotlin.String>
) {

}

