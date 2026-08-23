package be.icc.metamind.institution;

public record InstitutionResponse(
		long id,
		String code,
		String name,
		String emailDomain,
		boolean active
) {
	public static InstitutionResponse from(InstitutionEntity institution) {
		return new InstitutionResponse(
				institution.getId(),
				institution.getCode(),
				institution.getName(),
				institution.getEmailDomain(),
				institution.isActive()
		);
	}
}
