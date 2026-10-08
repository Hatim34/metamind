package be.icc.metamind.api;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class SpaFallbackFilter extends OncePerRequestFilter {
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		if (shouldForward(request)) {
			noCache(response);
			request.getRequestDispatcher("/index.html").forward(request, response);
			return;
		}
		if (isIndex(request)) {
			noCache(response);
		}
		filterChain.doFilter(request, response);
	}

	/**
	 * La page d'entree doit toujours etre redemandee : apres un deploiement, une ancienne copie
	 * gardee par le navigateur reclame des fichiers qui n'existent plus et l'application ne s'ouvre pas.
	 */
	private void noCache(HttpServletResponse response) {
		response.setHeader("Cache-Control", "no-cache");
	}

	private boolean isIndex(HttpServletRequest request) {
		String path = request.getRequestURI();
		return "GET".equalsIgnoreCase(request.getMethod()) && (path.equals("/") || path.equals("/index.html"));
	}

	private boolean shouldForward(HttpServletRequest request) {
		if (!"GET".equalsIgnoreCase(request.getMethod())) {
			return false;
		}

		String path = request.getRequestURI();
		String contextPath = request.getContextPath();
		if (!contextPath.isBlank() && path.startsWith(contextPath)) {
			path = path.substring(contextPath.length());
		}

		if (path.isBlank() || path.equals("/") || path.startsWith("/api/")) {
			return false;
		}

		String lastSegment = path.substring(path.lastIndexOf('/') + 1);
		return !lastSegment.contains(".");
	}
}
