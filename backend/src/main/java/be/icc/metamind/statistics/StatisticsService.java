package be.icc.metamind.statistics;

import be.icc.metamind.institution.InstitutionRepository;
import be.icc.metamind.publication.PublicationRepository;
import be.icc.metamind.publication.PublicationStatus;
import be.icc.metamind.publication.Visibility;
import be.icc.metamind.user.UserEntity;
import be.icc.metamind.user.UserRole;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatisticsService {
	private final PublicationRepository publicationRepository;
	private final InstitutionRepository institutionRepository;

	public StatisticsService(PublicationRepository publicationRepository, InstitutionRepository institutionRepository) {
		this.publicationRepository = publicationRepository;
		this.institutionRepository = institutionRepository;
	}

	@Transactional(readOnly = true)
	public StatisticsResponse getStatistics(UserEntity currentUser) {
		if (currentUser.getRole() == UserRole.ADMINISTRATEUR) {
			int creditBalance = institutionRepository.findAll().stream()
					.mapToInt(institution -> institution.getCreditBalance())
					.sum();
			return new StatisticsResponse(
					"GLOBAL",
					publicationRepository.count(),
					publicationRepository.countByStatus(PublicationStatus.PUBLIE),
					publicationRepository.countByStatus(PublicationStatus.A_VALIDER),
					publicationRepository.countByVisibility(Visibility.PUBLIC),
					publicationRepository.countByVisibility(Visibility.INSTITUTION),
					creditBalance
			);
		}

		return new StatisticsResponse(
				currentUser.getInstitution().getName(),
				publicationRepository.countByInstitution(currentUser.getInstitution()),
				publicationRepository.countByInstitutionAndStatus(currentUser.getInstitution(), PublicationStatus.PUBLIE),
				publicationRepository.countByInstitutionAndStatus(currentUser.getInstitution(), PublicationStatus.A_VALIDER),
				publicationRepository.countByInstitutionAndVisibility(currentUser.getInstitution(), Visibility.PUBLIC),
				publicationRepository.countByInstitutionAndVisibility(currentUser.getInstitution(), Visibility.INSTITUTION),
				currentUser.getInstitution().getCreditBalance()
		);
	}
}
