package be.icc.metamind.statistics;

import be.icc.metamind.document.DocumentRepository;
import be.icc.metamind.document.DocumentStatus;
import be.icc.metamind.document.DocumentVisibility;
import be.icc.metamind.institution.InstitutionRepository;
import be.icc.metamind.user.UserEntity;
import be.icc.metamind.user.UserRole;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatisticsService {
	private final DocumentRepository documentRepository;
	private final InstitutionRepository institutionRepository;

	public StatisticsService(DocumentRepository documentRepository, InstitutionRepository institutionRepository) {
		this.documentRepository = documentRepository;
		this.institutionRepository = institutionRepository;
	}

	@Transactional(readOnly = true)
	public StatisticsResponse getStatistics(UserEntity currentUser) {
		if (currentUser.getRole() == UserRole.ADMIN) {
			int creditBalance = institutionRepository.findAll().stream()
					.mapToInt(institution -> institution.getCreditBalance())
					.sum();
			return new StatisticsResponse(
					"GLOBAL",
					documentRepository.count(),
					documentRepository.countByStatus(DocumentStatus.PUBLIE),
					documentRepository.countByStatus(DocumentStatus.A_VALIDER),
					documentRepository.countByVisibility(DocumentVisibility.PUBLIC),
					documentRepository.countByVisibility(DocumentVisibility.INSTITUTION),
					creditBalance
			);
		}

		return new StatisticsResponse(
				currentUser.getInstitution().getName(),
				documentRepository.countByInstitution(currentUser.getInstitution()),
				documentRepository.countByInstitutionAndStatus(currentUser.getInstitution(), DocumentStatus.PUBLIE),
				documentRepository.countByInstitutionAndStatus(currentUser.getInstitution(), DocumentStatus.A_VALIDER),
				documentRepository.countByInstitutionAndVisibility(currentUser.getInstitution(), DocumentVisibility.PUBLIC),
				documentRepository.countByInstitutionAndVisibility(currentUser.getInstitution(), DocumentVisibility.INSTITUTION),
				currentUser.getInstitution().getCreditBalance()
		);
	}
}
