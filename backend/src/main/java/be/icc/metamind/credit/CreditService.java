package be.icc.metamind.credit;

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

	public CreditService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public CreditBalanceResponse getBalance(long userId) {
		return toResponse(findUser(userId).getInstitution());
	}

	@Transactional
	public CreditBalanceResponse purchase(long userId, CreditPurchaseRequest request) {
		InstitutionEntity institution = findUser(userId).getInstitution();
		institution.addCredits(request.amount());
		return toResponse(institution);
	}

	private UserEntity findUser(long userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Le compte utilisateur est introuvable."));
	}

	private CreditBalanceResponse toResponse(InstitutionEntity institution) {
		return new CreditBalanceResponse(institution.getId(), institution.getName(), institution.getCreditBalance());
	}
}
