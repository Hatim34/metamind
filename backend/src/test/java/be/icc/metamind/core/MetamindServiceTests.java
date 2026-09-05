package be.icc.metamind.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import be.icc.metamind.api.ApiException;
import be.icc.metamind.auth.AuthResponse;
import be.icc.metamind.auth.LoginRequest;
import be.icc.metamind.auth.RegisterRequest;
import be.icc.metamind.publication.PublicationResponse;
import be.icc.metamind.publication.PublicationStatus;
import be.icc.metamind.user.UpdateProfileRequest;
import be.icc.metamind.user.UserResponse;
import be.icc.metamind.user.UserStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class MetamindServiceTests {
	private MetamindService service;

	@BeforeEach
	void setUp() {
		service = new MetamindService();
	}

	@Test
	void findsPublicationsByKeyword() {
		List<PublicationResponse> publications = service.findPublications("Dublin");

		assertThat(publications).hasSize(1);
		assertThat(publications.getFirst().status()).isEqualTo(PublicationStatus.PUBLIE);
	}

	@Test
	void rejectsUnknownLoginEmail() {
		LoginRequest request = new LoginRequest("inconnu@institution-a.example", "558435");

		assertThatThrownBy(() -> service.login(request))
				.isInstanceOf(ApiException.class)
				.extracting("status")
				.isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void registersNewLibrarian() {
		RegisterRequest request = new RegisterRequest(
				"Amal",
				"Diallo",
				"amal@institution-a.example",
				"Institution A",
				"558435"
		);

		AuthResponse response = service.register(request);

		assertThat(response.user().email()).isEqualTo("amal@institution-a.example");
		assertThat(response.user().status()).isEqualTo(UserStatus.ACTIF);
		assertThat(response.token()).startsWith("eyJ");
	}

	@Test
	void rejectsDuplicatedEmail() {
		RegisterRequest request = new RegisterRequest(
				"Sarah",
				"Lemaire",
				"sarah@institution-a.example",
				"Institution A",
				"558435"
		);

		assertThatThrownBy(() -> service.register(request))
				.isInstanceOf(ApiException.class)
				.extracting("status")
				.isEqualTo(HttpStatus.CONFLICT);
	}

	@Test
	void updatesProfileWithoutChangingStatus() {
		UpdateProfileRequest request = new UpdateProfileRequest("Sarah", "Lemaire", "Institution A");

		UserResponse response = service.updateProfile(1L, request);

		assertThat(response.institution()).isEqualTo("Institution A");
		assertThat(response.status()).isEqualTo(UserStatus.ACTIF);
	}

	@Test
	void accountDeletionChangesStatusOnly() {
		UserResponse response = service.requestAccountDeletion(1L);

		assertThat(response.status()).isEqualTo(UserStatus.DESACTIVE);
		assertThat(response.email()).isEqualTo("compte-supprime-1@metamind.example");
	}
}
