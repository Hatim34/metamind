package be.icc.metamind.publication;

import java.util.List;

import be.icc.metamind.institution.InstitutionEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicationRepository extends JpaRepository<PublicationEntity, Long> {
	List<PublicationEntity> findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCaseOrKeywordsTextContainingIgnoreCase(String title, String author, String keywordsText);

	long countByInstitution(InstitutionEntity institution);

	long countByStatus(PublicationStatus status);

	long countByInstitutionAndStatus(InstitutionEntity institution, PublicationStatus status);

	long countByVisibility(Visibility visibility);

	long countByInstitutionAndVisibility(InstitutionEntity institution, Visibility visibility);
}
