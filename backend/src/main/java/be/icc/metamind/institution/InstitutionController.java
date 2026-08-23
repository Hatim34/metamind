package be.icc.metamind.institution;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/institutions")
public class InstitutionController {
	private final InstitutionService service;

	public InstitutionController(InstitutionService service) {
		this.service = service;
	}

	@GetMapping
	public List<InstitutionResponse> list() {
		return service.findAll();
	}

	@PostMapping
	public InstitutionResponse create(@Valid @RequestBody InstitutionRequest request) {
		return service.create(request);
	}

	@DeleteMapping("/{id}")
	public InstitutionResponse deactivate(@PathVariable long id) {
		return service.deactivate(id);
	}
}
