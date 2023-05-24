package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema

/**
 * List of parts of the body that have other marks. From REFERENCE_CODES table where DOMAIN = BODY_PART. Allowable values extracted 08/02/2023.
 * @param bodyPart Part of the body that has the mark. From REFERENCE_CODES table where DOMAIN = BODY_PART. Allowable values extracted 08/02/2023.
 * @param comment Optional free text comment describing the mark
 */
data class BodyPartDetail(

    @Schema(example = "Head", description = "Part of the body that has the mark. From REFERENCE_CODES table where DOMAIN = BODY_PART. Allowable values extracted 08/02/2023.")
    @get:JsonProperty("bodyPart") val bodyPart: BodyPartDetail.BodyPart? = null,

    @Schema(example = "Skull and crossbones covering chest", description = "Optional free text comment describing the mark")
    @get:JsonProperty("comment") val comment: kotlin.String? = null
) {

    /**
    * Part of the body that has the mark. From REFERENCE_CODES table where DOMAIN = BODY_PART. Allowable values extracted 08/02/2023.
    * Values: ANKLE,ARM,EAR,ELBOW,FACE,FINGER,FOOT,HAND,HEAD,KNEE,LEG,LIP,NECK,NOSE,SHOULDER,THIGH,TOE,TORSO
    */
    enum class BodyPart(val value: kotlin.String) {

        @JsonProperty("Ankle") ANKLE("Ankle"),
        @JsonProperty("Arm") ARM("Arm"),
        @JsonProperty("Ear") EAR("Ear"),
        @JsonProperty("Elbow") ELBOW("Elbow"),
        @JsonProperty("Face") FACE("Face"),
        @JsonProperty("Finger") FINGER("Finger"),
        @JsonProperty("Foot") FOOT("Foot"),
        @JsonProperty("Hand") HAND("Hand"),
        @JsonProperty("Head") HEAD("Head"),
        @JsonProperty("Knee") KNEE("Knee"),
        @JsonProperty("Leg") LEG("Leg"),
        @JsonProperty("Lip") LIP("Lip"),
        @JsonProperty("Neck") NECK("Neck"),
        @JsonProperty("Nose") NOSE("Nose"),
        @JsonProperty("Shoulder") SHOULDER("Shoulder"),
        @JsonProperty("Thigh") THIGH("Thigh"),
        @JsonProperty("Toe") TOE("Toe"),
        @JsonProperty("Torso") TORSO("Torso")
    }

}

