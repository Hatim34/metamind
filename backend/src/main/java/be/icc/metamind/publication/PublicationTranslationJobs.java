package be.icc.metamind.publication;

import java.util.List;

import be.icc.metamind.document.DocumentPublishedEvent;
import be.icc.metamind.document.DocumentRepository;
import be.icc.metamind.document.DocumentSummary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Prepare les traductions d'affichage hors de la requete de l'utilisateur.
 *
 * Changer de langue doit traduire tout le site, catalogue compris, sans attente :
 * chaque notice publiee est donc traduite une fois dans les autres langues, puis
 * servie depuis la base. Un echec n'empeche ni la publication ni l'affichage :
 * la notice reste dans sa langue d'origine.
 */
@Component
public class PublicationTranslationJobs {
	private static final Logger log = LoggerFactory.getLogger(PublicationTranslationJobs.class);
	private static final List<String> LANGUAGES = List.of("fr", "nl", "en");

	private final PublicationTranslationService translationService;
	private final DocumentRepository documentRepository;

	public PublicationTranslationJobs(PublicationTranslationService translationService, DocumentRepository documentRepository) {
		this.translationService = translationService;
		this.documentRepository = documentRepository;
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onPublished(DocumentPublishedEvent event) {
		translateEverywhere(event.documentId());
	}

	/** Traduit toutes les notices publiques deja publiees (reprise des donnees existantes). */
	@Async
	public void translateAllPublished() {
		List<Long> ids = documentRepository.findPublishedSummaries().stream().map(DocumentSummary::id).toList();
		log.info("Traduction de {} notices publiees", ids.size());
		ids.forEach(this::translateEverywhere);
		log.info("Traductions terminees");
	}

	private void translateEverywhere(long documentId) {
		for (String language : LANGUAGES) {
			try {
				// Deja traduite et inchangee : servie depuis le cache, sans appel au modele.
				translationService.translate(documentId, language, null);
			}
			catch (RuntimeException exception) {
				log.warn("Traduction {} de la notice {} impossible : {}", language, documentId, exception.getMessage());
			}
		}
	}
}
