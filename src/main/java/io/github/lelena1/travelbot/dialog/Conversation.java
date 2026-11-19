package io.github.lelena1.travelbot.dialog;

import java.time.Duration;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * На каком шаге анкеты находится пользователь. Хранится в базе,
 * поэтому перезапуск бота не сбрасывает недозаполненную поездку.
 */
@Entity
@Table(name = "conversation")
public class Conversation {

	@Id
	@Column(name = "user_id")
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private DialogStep step;

	@Column(name = "trip_id")
	private Long tripId;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Conversation() {
		// для JPA
	}

	public Conversation(long userId, long tripId, Instant now) {
		this.userId = userId;
		this.tripId = tripId;
		moveTo(DialogStep.DESTINATION, now);
	}

	public void moveTo(DialogStep next, Instant now) {
		this.step = next;
		this.updatedAt = now;
	}

	/** Пользователь не отвечал дольше {@code ttl}. */
	public boolean isExpired(Instant now, Duration ttl) {
		return updatedAt.plus(ttl).isBefore(now);
	}

	public Long getUserId() {
		return userId;
	}

	public DialogStep getStep() {
		return step;
	}

	public Long getTripId() {
		return tripId;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
