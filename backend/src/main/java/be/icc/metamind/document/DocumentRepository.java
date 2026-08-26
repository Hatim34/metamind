package be.icc.metamind.document;

import java.util.List;

import be.icc.metamind.institution.InstitutionEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {
	@Query("""
			select distinct d
			from DocumentEntity d
			left join MetadataEntity m on m.document = d
			left join DocumentAuthorEntity da on da.document = d
			left join da.author a
			left join DocumentKeywordEntity dk on dk.document = d
			left join dk.keyword k
			where lower(coalesce(m.titre, '')) like lower(concat('%', :search, '%'))
			   or lower(coalesce(a.fullName, '')) like lower(concat('%', :search, '%'))
			   or lower(coalesce(k.libelle, '')) like lower(concat('%', :search, '%'))
			   or lower(coalesce(d.extractedText, '')) like lower(concat('%', :search, '%'))
			""")
	List<DocumentEntity> search(@Param("search") String search);

	long countByInstitution(InstitutionEntity institution);

	long countByStatus(DocumentStatus status);

	long countByInstitutionAndStatus(InstitutionEntity institution, DocumentStatus status);

	long countByVisibility(DocumentVisibility visibility);

	long countByInstitutionAndVisibility(InstitutionEntity institution, DocumentVisibility visibility);
}
