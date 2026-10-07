package be.icc.metamind.publication;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import be.icc.metamind.api.ApiException;
import be.icc.metamind.document.DocumentPublishedEvent;
import be.icc.metamind.document.DocumentRepository;
import be.icc.metamind.document.DocumentSummary;
import be.icc.metamind.user.UserEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Prepare les traductions d'affichage hors de la requete de l'utilisateur.
 *
 * Changer de langue doit traduire tout le site, catalogue compris, sans attente :
 * chaque notice publiee est donc traduite une fois dans les autres langues, puis
 * servie depuis la base. Le modele est parfois sature : chaque traduction est
 * retentee, et une reprise periodique complete celles qui manquent encore.
 */
@Component
public class PublicationTranslationJobs {
	private static final Logger log = LoggerFactory.getLogger(PublicationTranslationJobs.class);
	private static final List<String> LANGUAGES = List.of("fr", "nl", "en");
	private static final int ATTEMPTS = 3;

	private final PublicationTranslationService translationService;
	private final DocumentRepository documentRepository;
	private final long retryPauseMillis;
	private final Set<Long> inProgress = ConcurrentHashMap.newKeySet();
	private final AtomicBoolean sweeping = new AtomicBoolean();

	public PublicationTranslationJobs(
			PublicationTranslationService translationService,
			DocumentRepository documentRepository,
			@Value("${metamind.translation.retry-pause-ms:20000}") long retryPauseMillis
	) {
		this.translationService = translationService;
		this.documentRepository = documentRepository;
		this.retryPauseMillis = retryPauseMillis;
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onPublished(DocumentPublishedEvent event) {
		translateEverywhere(event.documentId(), null);
	}

	/** Demandee par une fiche affichee avant que sa traduction soit prete. */
	@Async
	public void translateSoon(long documentId, UserEntity reader) {
		translateEverywhere(documentId, reader);
	}

	/** Reprise manuelle demandee par l'administrateur. */
	@Async
	public void translateAllPublished() {
		translateMissing();
	}

	/** Complete regulierement les traductions qui ont echoue (modele indisponible, delai depasse). */
	@Scheduled(
			initialDelayString = "${metamind.translation.sweep-initial-delay-ms:120000}",
			fixedDelayString = "${metamind.translation.sweep-interval-ms:900000}"
	)
	public void translateMissing() {
		if (!sweeping.compareAndSet(false, true)) {
			return;
		}
		try {
			List<Long> ids = documentRepository.findPublishedSummaries().stream().map(DocumentSummary::id).toList();
			log.info("Verification des traductions de {} notices publiees", ids.size());
			ids.forEach(id -> translateEverywhere(id, null));
			log.info("Verification des traductions terminee");
		}
		finally {
			sweeping.set(false);
		}
	}

	private void translateEverywhere(long documentId, UserEntity reader) {
		if (!inProgress.add(documentId)) {
			return;
		}
		try {
			for (String language : LANGUAGES) {
				translateWithRetries(documentId, language, reader);
			}
		}
		finally {
			inProgress.remove(documentId);
		}
	}

	private void translateWithRetries(long documentId, String language, UserEntity reader) {
		for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
			try {
				// Deja traduite et inchangee : servie depuis le cache, sans appel au modele.
				translationService.translate(documentId, language, reader);
				return;
			}
			catch (ApiException exception) {
				// Notice introuvable ou non publique : un nouvel essai ne changerait rien.
				if (exception.getStatus().is4xxClientError()) {
					return;
				}
				if (!pauseBefore(attempt, documentId, language, exception)) {
					return;
				}
			}
			catch (RuntimeException exception) {
				if (!pauseBefore(attempt, documentId, language, exception)) {
					return;
				}
			}
		}
	}

	private boolean pauseBefore(int attempt, long documentId, String language, RuntimeException exception) {
		if (attempt == ATTEMPTS) {
			log.warn("Traduction {} de la notice {} impossible apres {} essais : {}", language, documentId, ATTEMPTS, exception.getMessage());
			return false;
		}
		try {
			Thread.sleep(retryPauseMillis * attempt);
			return true;
		}
		catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			return false;
		}
	}
}
