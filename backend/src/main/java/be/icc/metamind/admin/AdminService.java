package be.icc.metamind.admin;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import be.icc.metamind.api.ApiException;
import be.icc.metamind.document.AuditLogEntity;
import be.icc.metamind.document.AuditLogRepository;
import be.icc.metamind.document.ConfigurationRepository;
import be.icc.metamind.institution.InstitutionRepository;
import be.icc.metamind.institution.InstitutionResponse;
import be.icc.metamind.user.UserEntity;
import be.icc.metamind.user.UserRepository;
import be.icc.metamind.user.UserResponse;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
	private final UserRepository userRepository;
	private final InstitutionRepository institutionRepository;
	private final ConfigurationRepository configurationRepository;
	private final AuditLogRepository auditLogRepository;

	public AdminService(UserRepository userRepository, InstitutionRepository institutionRepository, ConfigurationRepository configurationRepository, AuditLogRepository auditLogRepository) {
		this.userRepository = userRepository;
		this.institutionRepository = institutionRepository;
		this.configurationRepository = configurationRepository;
		this.auditLogRepository = auditLogRepository;
	}

	@Transactional(readOnly = true)
	public List<UserResponse> listUsers(Long institutionId) {
		return userRepository.findAll().stream()
				.filter(user -> institutionId == null || user.getInstitution().getId().equals(institutionId))
				.map(UserResponse::from)
				.toList();
	}

	@Transactional
	public UserResponse updateUser(long id, AdminUserUpdateRequest request, UserEntity admin) {
		UserEntity user = userRepository.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Le compte utilisateur est introuvable."));
		user.updateAdministration(request.role(), request.status());
		auditLogRepository.save(new AuditLogEntity(
				admin,
				"MODIFICATION_UTILISATEUR",
				"users",
				id,
				"Role ou statut modifie",
				"system"
		));
		return UserResponse.from(user);
	}

	@Transactional(readOnly = true)
	public List<InstitutionResponse> listInstitutions() {
		return institutionRepository.findAll().stream()
				.map(InstitutionResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public Map<String, String> readConfiguration() {
		Map<String, String> values = configurationRepository.findAll().stream()
				.collect(Collectors.toMap(item -> item.getCle(), item -> item.getValeur() == null ? "" : item.getValeur()));
		values.putIfAbsent("modele_llm", "gemini-2.5-flash-lite");
		values.putIfAbsent("taille_max_upload_mo", "128");
		values.putIfAbsent("prix_credit_eur", "0.50");
		return values;
	}

	@Transactional(readOnly = true)
	public List<AuditLogResponse> listLogs() {
		return auditLogRepository.findAllByOrderByCreatedAtDesc().stream()
				.map(AuditLogResponse::from)
				.toList();
	}
}
