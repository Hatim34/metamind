package be.icc.metamind.api;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Resout l'adresse IP du client de la requete HTTP en cours, pour la
 * journalisation d'audit (cf. livrable 16 - Strategie de securite : chaque
 * action sensible est tracee avec l'adresse IP).
 */
public final class ClientIpResolver {

	private ClientIpResolver() {
	}

	public static String current() {
		if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
			return "inconnue";
		}
		HttpServletRequest request = attributes.getRequest();
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			return forwarded.split(",")[0].trim();
		}
		String remote = request.getRemoteAddr();
		return remote == null || remote.isBlank() ? "inconnue" : remote;
	}
}
