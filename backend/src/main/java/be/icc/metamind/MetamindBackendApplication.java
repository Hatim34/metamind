package be.icc.metamind;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class MetamindBackendApplication {

	public static void main(String[] args) {
		// Les dates enregistrees (journal, credits, validations) sont a l'heure belge, ete comme hiver.
		TimeZone.setDefault(TimeZone.getTimeZone("Europe/Brussels"));
		SpringApplication.run(MetamindBackendApplication.class, args);
	}

}
