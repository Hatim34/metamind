package be.icc.metamind.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
	Optional<UserEntity> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCase(String email);

	/**
	 * Compte les administrateurs actifs autres que celui vise.
	 * Sert a garantir qu'il reste toujours quelqu'un pour administrer la plateforme.
	 */
	long countByRoleAndStatusAndIdNot(UserRole role, UserStatus status, Long id);
}
