package be.icc.metamind.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class ExtractionTaskEntityTests {
	@Test
	void canOnlyCancelATaskWaitingInTheQueue() {
		ExtractionTaskEntity task = new ExtractionTaskEntity(null, null, "lot-1", null);

		assertThat(task.cancel()).isTrue();
		assertThat(task.getStatus()).isEqualTo(ExtractionTaskStatus.ANNULE);
		assertThat(task.cancel()).isFalse();
	}

	@Test
	void schedulesARetryOrMarksTheTaskAsFailed() {
		ExtractionTaskEntity task = new ExtractionTaskEntity(null, null, "lot-1", null);

		task.failOrRetry(LocalDateTime.now().plusMinutes(2));
		assertThat(task.getStatus()).isEqualTo(ExtractionTaskStatus.EN_FILE);
		assertThat(task.getAttempts()).isEqualTo(1);

		task.failOrRetry(null);
		assertThat(task.getStatus()).isEqualTo(ExtractionTaskStatus.ECHEC);
		assertThat(task.getAttempts()).isEqualTo(2);
	}
}
