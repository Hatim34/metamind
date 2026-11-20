package be.icc.metamind.admin;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import be.icc.metamind.institution.InstitutionEntity;
import be.icc.metamind.institution.InstitutionRepository;
import be.icc.metamind.notification.AccountEvent;
import be.icc.metamind.institution.InstitutionResponse;
import be.icc.metamind.user.UserEntity;
import be.icc.metamind.user.UserRole;
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
		UserRole previousRole = user.getRole();
		String previousInstitution = user.getInstitution().getName();
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
				userChanges(user, previousRole, previousStatus, previousInstitution),
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
		List<String> changes = new ArrayList<>();
		rename(institution, request.nom(), changes);
		changeEmailDomain(institution, request.domaineEmail(), changes);
		if (request.actif() != null) {
			boolean wasRequested = institution.isPending();
			if (wasRequested) {
				changes.add(request.actif() ? "Demande d'institution validée" : "Demande d'institution refusée");
			} else if (request.actif() != institution.isActive()) {
				changes.add(request.actif() ? "Institution réactivée" : "Institution désactivée");
			}
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
		if (request.purchasesSuspended() != null && request.purchasesSuspended() != institution.isPurchasesSuspended()) {
			changes.add(request.purchasesSuspended() ? "Achats de crédits suspendus" : "Achats de crédits réautorisés");
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
			changes.add("20 crédits de bienvenue accordés");
		}
		auditLogRepository.save(new AuditLogEntity(
				admin,
				"MODIFICATION_INSTITUTION",
				"institutions",
				id,
				changes.isEmpty() ? "Aucun changement" : String.join(" ; ", changes),
				ClientIpResolver.current()
		));
		return InstitutionResponse.from(institution);
	}

	private void rename(InstitutionEntity institution, String requestedName, List<String> changes) {
		if (requestedName == null) {
			return;
		}
		String name = requestedName.trim();
		if (name.isBlank()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Le nom de l'institution est obligatoire.");
		}
		if (name.equals(institution.getName())) {
			return;
		}
		institutionRepository.findByNameIgnoreCase(name)
				.filter(other -> !other.getId().equals(institution.getId()))
				.ifPresent(other -> {
					throw new ApiException(HttpStatus.CONFLICT, "Une autre institution porte deja ce nom.");
				});
		changes.add("Nom : " + institution.getName() + " → " + name);
		institution.rename(name);
	}

	/**
	 * Le domaine rattache les comptes a l'institution : il ne change que tant qu'aucun compte n'y est rattache,
	 * sinon des adresses existantes ne correspondraient plus au domaine de leur institution.
	 */
	private void changeEmailDomain(InstitutionEntity institution, String requestedDomain, List<String> changes) {
		if (requestedDomain == null) {
			return;
		}
		String domain = requestedDomain.trim().toLowerCase();
		if (!domain.matches("[a-z0-9-]+(\\.[a-z0-9-]+)+")) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Le domaine email n'est pas valide (exemple : ulb.be).");
		}
		if (domain.equalsIgnoreCase(institution.getEmailDomain())) {
			return;
		}
		if (userRepository.countByInstitution_Id(institution.getId()) > 0) {
			throw new ApiException(HttpStatus.CONFLICT, "Le domaine ne peut pas changer : des comptes sont rattaches a cette institution.");
		}
		institutionRepository.findByEmailDomainIgnoreCase(domain)
				.filter(other -> !other.getId().equals(institution.getId()))
				.ifPresent(other -> {
					throw new ApiException(HttpStatus.CONFLICT, "Ce domaine est deja utilise par une autre institution.");
				});
		changes.add("Domaine : " + institution.getEmailDomain() + " → " + domain);
		institution.changeEmailDomain(domain);
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
		Map<String, String> previous = platformSettings.current();
		values.forEach((key, value) -> updateConfigurationValue(cleanKey(key), cleanValue(value), admin));
		Map<String, String> current = platformSettings.current();
		List<String> changes = values.keySet().stream()
				.map(String::trim)
				.filter(key -> !Objects.equals(previous.get(key), current.get(key)))
				.map(key -> key + " : " + previous.get(key) + " → " + current.get(key))
				.toList();
		auditLogRepository.save(new AuditLogEntity(
				admin,
				"MODIFICATION_CONFIGURATION",
				"configurations",
				null,
				changes.isEmpty() ? "Aucun changement" : String.join(" ; ", changes),
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
	public PageResponse<AuditLogResponse> listLogs(int page, int size, String query, LocalDate from, LocalDate to) {
		Map<Long, String> users = userRepository.findAll().stream()
				.collect(Collectors.toMap(UserEntity::getId, user -> user.getFirstName() + " " + user.getLastName()));
		Map<Long, String> institutions = institutionRepository.findAll().stream()
				.collect(Collectors.toMap(institution -> institution.getId(), institution -> institution.getName()));
		Map<Long, String> titles = metadataRepository.findAll().stream()
				.filter(metadata -> metadata.getTitre() != null)
				.collect(Collectors.toMap(metadata -> metadata.getDocument().getId(), MetadataEntity::getTitre, (first, second) -> first));
		String filter = query == null ? "" : query.trim().toLowerCase();
		List<AuditLogResponse> logs = auditLogRepository.findAllByOrderByCreatedAtDesc().stream()
				// Periode inclusive : du premier jour a 0 h jusqu'a la fin du dernier jour.
				.filter(log -> from == null || !log.getCreatedAt().toLocalDate().isBefore(from))
				.filter(log -> to == null || !log.getCreatedAt().toLocalDate().isAfter(to))
				.map(log -> AuditLogResponse.from(log, entityLabel(log, users, institutions, titles)))
				.filter(log -> filter.isEmpty() || String.join(" ",
						String.valueOf(log.action()), String.valueOf(log.entityLabel()), String.valueOf(log.auteur()),
						String.valueOf(log.details()), String.valueOf(log.entityId())).toLowerCase().contains(filter))
				.toList();
		return PageResponse.from(logs, page, size);
	}

	private String entityLabel(AuditLogEntity log, Map<Long, String> users, Map<Long, String> institutions, Map<Long, String> titles) {
		if (log.getEntityId() == null || log.getEntityType() == null) {
			return null;
		}
		return switch (log.getEntityType()) {
			case "users" -> users.get(log.getEntityId());
			case "institutions" -> institutions.get(log.getEntityId());
			case "metadonnees", "documents" -> titles.get(log.getEntityId());
			default -> null;
		};
	}

	private String userChanges(UserEntity user, UserRole previousRole, UserStatus previousStatus, String previousInstitution) {
		List<String> changes = new ArrayList<>();
		if (previousStatus != user.getStatus()) {
			changes.add("Statut : " + statusLabel(previousStatus) + " → " + statusLabel(user.getStatus()));
		}
		if (previousRole != user.getRole()) {
			changes.add("Rôle : " + roleLabel(previousRole) + " → " + roleLabel(user.getRole()));
		}
		if (!previousInstitution.equals(user.getInstitution().getName())) {
			changes.add("Institution : " + previousInstitution + " → " + user.getInstitution().getName());
		}
		return changes.isEmpty() ? "Aucun changement" : String.join(" ; ", changes);
	}

	private String statusLabel(UserStatus status) {
		return switch (status) {
			case EN_ATTENTE -> "En attente";
			case ACTIF -> "Actif";
			case DESACTIVE -> "Désactivé";
		};
	}

	private String roleLabel(UserRole role) {
		return role == UserRole.ADMIN ? "Administrateur" : "Bibliothécaire";
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
