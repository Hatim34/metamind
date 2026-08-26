package be.icc.metamind.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Transactional
class DatabaseSchemaAlignmentTests {
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void createsExpectedSixteenTables() {
		Set<String> tables = Set.copyOf(jdbcTemplate.queryForList(
				"select lower(table_name) from information_schema.tables where table_schema = 'PUBLIC'",
				String.class
		));

		assertThat(tables).contains(
				"institutions",
				"users",
				"documents",
				"metadonnees",
				"langues",
				"types_documents",
				"auteurs",
				"documents_auteurs",
				"mots_cles",
				"documents_mots_cles",
				"enrichissements",
				"suggestions_metadonnees",
				"logs_audit",
				"configurations",
				"packs_credits",
				"mouvements_credits"
		);
	}
}
