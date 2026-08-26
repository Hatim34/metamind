package be.icc.metamind.institution;

import java.util.List;

import be.icc.metamind.api.ApiException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstitutionService {
	private final InstitutionRepository repository;

	public InstitutionService(InstitutionRepository repository) {
		this.repository = repository;
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

		InstitutionEntity institution = new InstitutionEntity(request.code(), request.name(), request.emailDomain().toLowerCase());
		return InstitutionResponse.from(repository.save(institution));
	}

	@Transactional
	public InstitutionResponse deactivate(long id) {
		InstitutionEntity institution = repository.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "L'institution demandee est introuvable."));
		institution.deactivate();
		return InstitutionResponse.from(institution);
	}
}
