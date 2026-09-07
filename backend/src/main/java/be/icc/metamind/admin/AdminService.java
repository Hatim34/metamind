package be.icc.metamind.admin;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import be.icc.metamind.api.ApiException;
import be.icc.metamind.api.PageResponse;
import be.icc.metamind.document.AuditLogEntity;
import be.icc.metamind.document.AuditLogRepository;
import be.icc.metamind.document.ConfigurationEntity;
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
	private static final Set<String> CONFIGURATION_KEYS = Set.of(
			"modele_llm",
			"fournisseur_llm",
			"url_dspace",
			"taille_max_upload_mo",
			"langues",
			"cle_api_llm"
	);

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
	public PageResponse<UserResponse> listUsers(Long institutionId, int page, int size) {
		List<UserResponse> users = userRepository.findAll().stream()
				.filter(user -> institutionId == null || user.getInstitution().getId().equals(institutionId))
				.map(UserResponse::from)
				.toList();
		return PageResponse.from(users, page, size);
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

	@Transactional
	public Map<String, String> updateConfiguration(Map<String, String> values, UserEntity admin) {
		if (values == null || values.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Aucun parametre de configuration n'a ete fourni.");
		}
		values.forEach((key, value) -> updateConfigurationValue(cleanKey(key), cleanValue(value), admin));
		auditLogRepository.save(new AuditLogEntity(
				admin,
				"MODIFICATION_CONFIGURATION",
				"configurations",
				null,
				"Parametres modifies : " + String.join(", ", values.keySet()),
				"system"
		));
		return readConfiguration();
	}

	private void updateConfigurationValue(String key, String value, UserEntity admin) {
		ConfigurationEntity configuration = configurationRepository.findById(key)
				.orElseGet(() -> new ConfigurationEntity(key, value, admin));
		configuration.update(value, admin);
		configurationRepository.save(configuration);
	}

	private String cleanKey(String value) {
		if (value == null || value.trim().isBlank()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "La cle de configuration est obligatoire.");
		}
		String key = value.trim();
		if (!CONFIGURATION_KEYS.contains(key)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "La cle de configuration n'est pas autorisee.");
		}
		return key;
	}

	private String cleanValue(String value) {
		if (value == null || value.trim().isBlank()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "La valeur de configuration est obligatoire.");
		}
		return value.trim();
	}

	@Transactional(readOnly = true)
	public PageResponse<AuditLogResponse> listLogs(int page, int size) {
		List<AuditLogResponse> logs = auditLogRepository.findAllByOrderByCreatedAtDesc().stream()
				.map(AuditLogResponse::from)
				.toList();
		return PageResponse.from(logs, page, size);
	}
}
