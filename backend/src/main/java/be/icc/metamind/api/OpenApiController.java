package be.icc.metamind.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OpenApiController {
	@GetMapping(value = "/api/v1/openapi.yaml", produces = "application/yaml")
	public String openApi() throws IOException {
		ClassPathResource resource = new ClassPathResource("openapi.yaml");
		return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
	}
}
