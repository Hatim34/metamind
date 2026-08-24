package be.icc.metamind.opendata;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/open-data")
public class OpenDataController {
	private final OpenDataService service;

	public OpenDataController(OpenDataService service) {
		this.service = service;
	}

	@GetMapping(value = "/rss", produces = "application/rss+xml")
	public String rssFeed() {
		return service.rssFeed();
	}

	@GetMapping(value = "/publications/{id}/dublin-core", produces = MediaType.APPLICATION_XML_VALUE)
	public String dublinCore(@PathVariable long id) {
		return service.dublinCore(id);
	}
}
