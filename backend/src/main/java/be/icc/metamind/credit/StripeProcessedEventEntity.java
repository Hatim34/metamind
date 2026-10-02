package be.icc.metamind.credit;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "evenements_stripe_traites")
public class StripeProcessedEventEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "event_id", nullable = false, unique = true, length = 255)
	private String eventId;

	@Column(name = "type_evenement", nullable = false, length = 100)
	private String eventType;

	@Column(name = "traite_at", nullable = false)
	private Instant processedAt;

	protected StripeProcessedEventEntity() {
	}

	public StripeProcessedEventEntity(String eventId, String eventType, Instant processedAt) {
		this.eventId = eventId;
		this.eventType = eventType;
		this.processedAt = processedAt;
	}
}
