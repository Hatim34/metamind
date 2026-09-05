package be.icc.metamind.api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PageResponse<T>(
		List<T> contenu,
		int page,
		int size,
		@JsonProperty("total_elements")
		long totalElements,
		@JsonProperty("total_pages")
		int totalPages
) {
	public static <T> PageResponse<T> from(List<T> allItems, int page, int size) {
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), 100);
		int from = Math.min(safePage * safeSize, allItems.size());
		int to = Math.min(from + safeSize, allItems.size());
		int pages = allItems.isEmpty() ? 0 : (int) Math.ceil((double) allItems.size() / safeSize);
		return new PageResponse<>(allItems.subList(from, to), safePage, safeSize, allItems.size(), pages);
	}
}
