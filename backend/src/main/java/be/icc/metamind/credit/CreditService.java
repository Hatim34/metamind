package be.icc.metamind.credit;

import java.util.List;

import be.icc.metamind.api.ApiException;
import be.icc.metamind.institution.InstitutionEntity;
import be.icc.metamind.user.UserEntity;
import be.icc.metamind.user.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreditService {
	private final UserRepository userRepository;
	private final CreditMovementRepository movementRepository;

	public CreditService(UserRepository userRepository, CreditMovementRepository movementRepository) {
		this.userRepository = userRepository;
		this.movementRepository = movementRepository;
	}

	@Transactional(readOnly = true)
	public CreditBalanceResponse getBalance(long userId) {
		return toResponse(findUser(userId).getInstitution());
	}

	@Transactional
	public CreditBalanceResponse purchase(long userId, CreditPurchaseRequest request) {
		InstitutionEntity institution = findUser(userId).getInstitution();
		institution.addCredits(request.amount());
		movementRepository.save(new CreditMovementEntity(
				institution,
				CreditMovementType.ACHAT,
				request.amount(),
				institution.getCreditBalance(),
				"Achat de credits"
		));
		return toResponse(institution);
	}

	@Transactional(readOnly = true)
	public List<CreditMovementResponse> listMovements(long userId) {
		InstitutionEntity institution = findUser(userId).getInstitution();
		return movementRepository.findByInstitutionIdOrderByCreatedAtDesc(institution.getId()).stream()
				.map(CreditMovementResponse::from)
				.toList();
	}

	private UserEntity findUser(long userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Le compte utilisateur est introuvable."));
	}

	private CreditBalanceResponse toResponse(InstitutionEntity institution) {
		return new CreditBalanceResponse(institution.getId(), institution.getName(), institution.getCreditBalance());
	}
}
