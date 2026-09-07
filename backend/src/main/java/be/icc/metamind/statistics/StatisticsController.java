package be.icc.metamind.statistics;

import java.time.LocalDate;

import be.icc.metamind.user.AccountService;
import be.icc.metamind.user.UserEntity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/stats", "/api/v1/statistics"})
public class StatisticsController {
	private final StatisticsService service;
	private final AccountService accountService;

	public StatisticsController(StatisticsService service, AccountService accountService) {
		this.service = service;
		this.accountService = accountService;
	}

	@GetMapping
	public StatisticsResponse getStatistics(
			@RequestHeader("Authorization") String authorization,
			@RequestParam(value = "date_debut", required = false) LocalDate startDate,
			@RequestParam(value = "date_fin", required = false) LocalDate endDate
	) {
		UserEntity currentUser = accountService.authenticate(authorization);
		return service.getStatistics(currentUser, startDate, endDate);
	}
}
