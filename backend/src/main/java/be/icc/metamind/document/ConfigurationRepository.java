package be.icc.metamind.document;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfigurationRepository extends JpaRepository<ConfigurationEntity, String> {
}
