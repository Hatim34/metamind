package be.icc.metamind.institution;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Photo publique d'une institution, avec le credit a afficher a cote. */
public record InstitutionPhotoResponse(
		@JsonProperty("institution_id") long institutionId,
		@JsonProperty("nom") String name,
		@JsonProperty("photo_url") String photoUrl,
		@JsonProperty("credit") String credit
) {
}
