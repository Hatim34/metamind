package be.icc.metamind.institution;

import java.util.List;

import be.icc.metamind.api.ApiException;
import be.icc.metamind.credit.CreditMovementEntity;
import be.icc.metamind.credit.CreditMovementRepository;
import be.icc.metamind.credit.CreditMovementType;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstitutionService {
	private static final int WELCOME_CREDITS = 20;

	private final InstitutionRepository repository;
	private final CreditMovementRepository creditMovementRepository;

	public InstitutionService(InstitutionRepository repository, CreditMovementRepository creditMovementRepository) {
		this.repository = repository;
		this.creditMovementRepository = creditMovementRepository;
	}

	public List<InstitutionResponse> findAll() {
		return repository.findAll()
				.stream()
				.map(InstitutionResponse::from)
				.toList();
	}

	@Transactional
	public InstitutionResponse create(InstitutionRequest request) {
		if (repository.findByNameIgnoreCase(request.name()).isPresent()) {
			throw new ApiException(HttpStatus.CONFLICT, "Une institution existe deja avec ce nom.");
		}

		if (repository.existsByEmailDomainIgnoreCase(request.emailDomain())) {
			throw new ApiException(HttpStatus.CONFLICT, "Une institution existe deja avec ce domaine email.");
		}

		InstitutionEntity institution = repository.save(new InstitutionEntity(request.code(), request.name(), request.emailDomain().toLowerCase()));
		// Meme offre de bienvenue qu'une institution validee apres une demande d'inscription.
		if (institution.grantWelcomeCredits(WELCOME_CREDITS)) {
			creditMovementRepository.save(new CreditMovementEntity(institution, CreditMovementType.OFFRE_BIENVENUE, WELCOME_CREDITS,
					institution.getCreditBalance(), "Offre de bienvenue accordee a la creation de l'institution"));
		}
		return InstitutionResponse.from(institution);
	}

	@Transactional
	public InstitutionResponse deactivate(long id) {
		InstitutionEntity institution = repository.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "L'institution demandee est introuvable."));
		institution.deactivate();
		return InstitutionResponse.from(institution);
	}
}
