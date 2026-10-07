package be.icc.metamind.api;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Ajoute les en-tetes de securite HTTP recommandes (OWASP A05, A03).
 * Couvre le clickjacking, le sniffing de type MIME, la politique de contenu (CSP)
 * et le transport securise (HSTS). Applique a toutes les reponses.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter extends OncePerRequestFilter {

	private static final String CONTENT_SECURITY_POLICY = String.join("; ",
			"default-src 'self'",
			"script-src 'self'",
			"style-src 'self' 'unsafe-inline' https://fonts.googleapis.com",
			"img-src 'self' data: blob:",
			"font-src 'self' data: https://fonts.gstatic.com",
			"connect-src 'self'",
			// Le PDF et la couverture sont telecharges avec le jeton puis affiches depuis une
			// adresse blob: creee par la page. Sans ces deux directives, Chrome affichait
			// « Ce contenu est bloque » a la place du document.
			"frame-src 'self' blob:",
			"frame-ancestors 'none'",
			"base-uri 'self'",
			"form-action 'self'",
			"object-src 'self' blob:");

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		response.setHeader("X-Content-Type-Options", "nosniff");
		response.setHeader("X-Frame-Options", "DENY");
		response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
		response.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
		response.setHeader("Content-Security-Policy", CONTENT_SECURITY_POLICY);
		if (request.isSecure()) {
			response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
		}
		filterChain.doFilter(request, response);
	}
}
