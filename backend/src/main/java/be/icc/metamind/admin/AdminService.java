package be.icc.metamind.admin;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import be.icc.metamind.api.ApiException;
import be.icc.metamind.api.ClientIpResolver;
import be.icc.metamind.api.PageResponse;
import be.icc.metamind.config.PlatformSettings;
import be.icc.metamind.document.AuditLogEntity;
import be.icc.metamind.document.AuditLogRepository;
import be.icc.metamind.document.ConfigurationEntity;
import be.icc.metamind.document.ConfigurationRepository;
import be.icc.metamind.document.DocumentRepository;
import be.icc.metamind.document.MetadataEntity;
import be.icc.metamind.document.MetadataRepository;
import be.icc.metamind.credit.CreditMovementEntity;
import be.icc.metamind.credit.CreditMovementRepository;
import be.icc.metamind.credit.CreditMovementType;
import be.icc.metamind.institution.InstitutionRepository;
import be.icc.metamind.notification.AccountEvent;
import be.icc.metamind.institution.InstitutionResponse;
import be.icc.metamind.user.UserEntity;
import be.icc.metamind.user.UserStatus;
import be.icc.metamind.user.AdministratorGuard;
import be.icc.metamind.user.UserRepository;
import be.icc.metamind.user.UserResponse;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
	private final UserRepository userRepository;
	private final ApplicationEventPublisher events;
	private final InstitutionRepository institutionRepository;
	private final ConfigurationRepository configurationRepository;
	private final AuditLogRepository auditLogRepository;
	private final DocumentRepository documentRepository;
	private final MetadataRepository metadataRepository;
	private final CreditMovementRepository creditMovementRepository;
	private final AdministratorGuard administratorGuard;
	private final PlatformSettings platformSettings;

	public AdminService(
			UserRepository userRepository,
			InstitutionRepository institutionRepository,
			ConfigurationRepository configurationRepository,
			AuditLogRepository auditLogRepository,
			DocumentRepository documentRepository,
			MetadataRepository metadataRepository,
			CreditMovementRepository creditMovementRepository,
			AdministratorGuard administratorGuard,
			PlatformSettings platformSettings,
			ApplicationEventPublisher events
	) {
		this.events = events;
		this.userRepository = userRepository;
		this.institutionRepository = institutionRepository;
		this.configurationRepository = configurationRepository;
		this.auditLogRepository = auditLogRepository;
		this.documentRepository = documentRepository;
		this.metadataRepository = metadataRepository;
		this.creditMovementRepository = creditMovementRepository;
		this.administratorGuard = administratorGuard;
		this.platformSettings = platformSettings;
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
		// Un administrateur ne se retire pas lui-meme ses droits : un autre administrateur doit le faire.
		if (user.getId().equals(admin.getId()) && (request.role() != null || request.status() != null)) {
			throw new ApiException(HttpStatus.CONFLICT, "Vous ne pouvez pas modifier votre propre role ou statut.");
		}
		// Retrograder ou desactiver le dernier administrateur rendrait la plateforme iningerable.
		if (administratorGuard.removesAdministration(request.role(), request.status())) {
			administratorGuard.ensureAnotherActiveAdministratorRemains(user, "modifier ce compte");
		}
		if (request.status() == UserStatus.ACTIF && user.getInstitution().isPending()) {
			throw new ApiException(HttpStatus.CONFLICT, "Validez d'abord la demande d'institution de ce compte.");
		}
		UserStatus previousStatus = user.getStatus();
		user.updateAdministration(request.role(), request.status());
		if (request.status() != null && request.status() != previousStatus) {
			events.publishEvent(new AccountEvent.StatusChanged(user.getId(), previousStatus, request.status()));
		}
		if (request.institutionId() != null) {
			var institution = institutionRepository.findById(request.institutionId())
					.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "L'institution est introuvable."));
			// Le rattachement suit toujours l'adresse email : sinon le compte publierait au nom d'une autre institution.
			if (!user.getEmail().toLowerCase().endsWith("@" + institution.getEmailDomain().toLowerCase())) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "L'adresse email de ce compte ne correspond pas au domaine de cette institution.");
			}
			user.assignInstitution(institution);
		}
		auditLogRepository.save(new AuditLogEntity(
				admin,
				"MODIFICATION_UTILISATEUR",
				"users",
				id,
				request.institutionId() == null ? "Role ou statut modifie" : "Role, statut ou institution modifie",
				ClientIpResolver.current()
		));
		return UserResponse.from(user);
	}

	@Transactional(readOnly = true)
	public List<InstitutionResponse> listInstitutions() {
		return institutionRepository.findAll().stream()
				.map(InstitutionResponse::from)
				.toList();
	}

	@Transactional
	public InstitutionResponse updateInstitution(long id, AdminInstitutionUpdateRequest request, UserEntity admin) {
		var institution = institutionRepository.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "L'institution est introuvable."));
		if (request.actif() != null) {
			boolean wasRequested = institution.isPending();
			if (request.actif()) {
				institution.activate();
			} else if (wasRequested) {
				institution.refuseRequest();
			} else {
				institution.deactivate();
			}
			// Valider ou refuser une demande d'institution tranche aussi les comptes qui l'ont demandee.
			if (wasRequested) {
				UserStatus decision = request.actif() ? UserStatus.ACTIF : UserStatus.DESACTIVE;
				userRepository.findAll().stream()
						.filter(user -> user.getInstitution().getId().equals(institution.getId()))
						.filter(user -> user.getStatus() == UserStatus.EN_ATTENTE)
						.forEach(user -> {
							user.updateAdministration(null, decision);
							events.publishEvent(new AccountEvent.StatusChanged(user.getId(), UserStatus.EN_ATTENTE, decision));
						});
			}
		}
		if (request.purchasesSuspended() != null) {
			institution.suspendPurchases(request.purchasesSuspended());
		}
		if (Boolean.TRUE.equals(request.actif()) && institution.grantWelcomeCredits(20)) {
			creditMovementRepository.save(new CreditMovementEntity(
					institution,
					CreditMovementType.OFFRE_BIENVENUE,
					20,
					institution.getCreditBalance(),
					"Offre de bienvenue accordee lors de l'activation"
			));
		}
		auditLogRepository.save(new AuditLogEntity(
				admin,
				"MODIFICATION_INSTITUTION",
				"institutions",
				id,
				"Activation ou suspension des achats modifiee",
				ClientIpResolver.current()
		));
		return InstitutionResponse.from(institution);
	}

	@Transactional(readOnly = true)
	public Map<String, String> readConfiguration() {
		return platformSettings.current();
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
				ClientIpResolver.current()
		));
		return readConfiguration();
	}

	private void updateConfigurationValue(String key, String value, UserEntity admin) {
		platformSettings.validate(key, value);
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
		if (!PlatformSettings.isEditable(key)) {
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

	@Transactional(readOnly = true)
	public String exportDocumentsCsv() {
		StringBuilder csv = new StringBuilder("id,titre,statut,visibilite,institution,date_publication,classification\n");
		documentRepository.findAll().forEach(document -> {
			MetadataEntity metadata = metadataRepository.findByDocumentId(document.getId()).orElse(null);
			csv.append(document.getId()).append(',')
					.append(csvValue(metadata == null ? document.getFileName() : metadata.getTitre())).append(',')
					.append(csvValue(document.getStatus().name())).append(',')
					.append(csvValue(document.getVisibility().name())).append(',')
					.append(csvValue(document.getInstitution().getName())).append(',')
					.append(csvValue(metadata == null || metadata.getPublicationDate() == null ? "" : metadata.getPublicationDate().toString())).append(',')
					.append(csvValue(metadata == null ? "" : metadata.getClassification()))
					.append('\n');
		});
		return csv.toString();
	}

	private String csvValue(String value) {
		String cleanValue = value == null ? "" : value;
		return "\"" + cleanValue.replace("\"", "\"\"") + "\"";
	}
}
