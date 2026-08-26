package be.icc.metamind.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import be.icc.metamind.api.ApiException;
import be.icc.metamind.auth.AuthResponse;
import be.icc.metamind.auth.LoginRequest;
import be.icc.metamind.auth.RegisterRequest;
import be.icc.metamind.publication.Publication;
import be.icc.metamind.publication.PublicationResponse;
import be.icc.metamind.publication.PublicationStatus;
import be.icc.metamind.publication.Visibility;
import be.icc.metamind.user.UpdateProfileRequest;
import be.icc.metamind.user.UserAccount;
import be.icc.metamind.user.UserResponse;
import be.icc.metamind.user.UserStatus;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class MetamindService {
	private final AtomicLong userIds = new AtomicLong(2);
	private final Map<Long, UserAccount> users = new ConcurrentHashMap<>();
	private final List<Publication> publications = new ArrayList<>();

	public MetamindService() {
		users.put(1L, new UserAccount(1L, "Sarah", "Lemaire", "sarah@institution-a.example", "Bibliothecaire", "Institution A", UserStatus.ACTIF));
		users.put(2L, new UserAccount(2L, "Nadia", "Benali", "admin@metamind.example", "Administrateur", "Metamind", UserStatus.ACTIF));

		publications.add(new Publication(1L, "Analyse automatique des metadonnees pour les depots institutionnels", "Sarah Lemaire", "Institution A", 2026, PublicationStatus.PUBLIE, Visibility.PUBLIC, List.of("Dublin Core", "metadonnees", "recherche")));
		publications.add(new Publication(2L, "Validation humaine des suggestions produites par un modele de langage", "Jan Peeters", "Institution B", 2025, PublicationStatus.A_VALIDER, Visibility.INSTITUTION, List.of("validation", "catalogage", "qualite")));
		publications.add(new Publication(3L, "Indexation multilingue de publications scientifiques", "Mina Laurent", "Institution A", 2024, PublicationStatus.PUBLIE, Visibility.PUBLIC, List.of("indexation", "recherche", "multilingue")));
	}

	public List<PublicationResponse> findPublications(String search) {
		String value = Optional.ofNullable(search).orElse("").trim().toLowerCase(Locale.ROOT);
		return publications.stream()
				.filter(publication -> value.isBlank() || publication.matches(value))
				.map(PublicationResponse::from)
				.toList();
	}

	public PublicationResponse findPublication(long id) {
		return publications.stream()
				.filter(publication -> publication.id() == id)
				.findFirst()
				.map(PublicationResponse::from)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "La publication demandee est introuvable."));
	}

	public AuthResponse login(LoginRequest request) {
		UserAccount user = users.values()
				.stream()
				.filter(account -> account.email().equalsIgnoreCase(request.email()))
				.findFirst()
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Les identifiants sont incorrects."));

		return new AuthResponse(localToken(user.id()), UserResponse.from(user));
	}

	public AuthResponse register(RegisterRequest request) {
		boolean emailExists = users.values()
				.stream()
				.anyMatch(user -> user.email().equalsIgnoreCase(request.email()));

		if (emailExists) {
			throw new ApiException(HttpStatus.CONFLICT, "Un compte existe deja avec cet email.");
		}

		UserAccount user = createUser(request.firstName(), request.lastName(), request.email(), request.institution());
		return new AuthResponse(localToken(user.id()), UserResponse.from(user));
	}

	public UserResponse getProfile(long id) {
		return UserResponse.from(findUser(id));
	}

	public UserResponse updateProfile(long id, UpdateProfileRequest request) {
		UserAccount user = findUser(id);
		UserAccount updated = new UserAccount(
				user.id(),
				request.firstName(),
				request.lastName(),
				user.email(),
				user.role(),
				request.institution(),
				user.status()
		);
		users.put(id, updated);
		return UserResponse.from(updated);
	}

	public UserResponse requestAccountDeletion(long id) {
		UserAccount user = findUser(id);
		UserAccount updated = new UserAccount(
				user.id(),
				"Compte",
				"Supprime",
				"compte-supprime-" + user.id() + "@metamind.local",
				user.role(),
				user.institution(),
				UserStatus.DESACTIVE
		);
		users.put(id, updated);
		return UserResponse.from(updated);
	}

	private UserAccount findUser(long id) {
		UserAccount user = users.get(id);
		if (user == null) {
			throw new ApiException(HttpStatus.NOT_FOUND, "Le compte utilisateur est introuvable.");
		}
		return user;
	}

	private UserAccount createUser(String firstName, String lastName, String email, String institution) {
		long id = userIds.incrementAndGet();
		UserAccount user = new UserAccount(id, firstName, lastName, email, "Bibliothecaire", institution, UserStatus.ACTIF);
		users.put(id, user);
		return user;
	}

	private String localToken(long userId) {
		return "eyJhbGciOiJIUzI1NiJ9.local-" + userId + ".signature";
	}
}
