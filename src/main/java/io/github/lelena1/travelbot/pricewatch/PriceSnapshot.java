package io.github.lelena1.travelbot.pricewatch;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Цена при одной проверке — история для графика. */
@Entity
@Table(name = "price_snapshot")
public class PriceSnapshot {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "watch_id", nullable = false)
	private Long watchId;

	/** null — в эту проверку цен не нашлось. */
	private Integer price;

	@Column(name = "checked_at", nullable = false)
	private Instant checkedAt;

	protected PriceSnapshot() {
		// для JPA
	}

	public PriceSnapshot(long watchId, Integer price, Instant checkedAt) {
		this.watchId = watchId;
		this.price = price;
		this.checkedAt = checkedAt;
	}

	public Long getId() {
		return id;
	}

	public Long getWatchId() {
		return watchId;
	}

	public Integer getPrice() {
		return price;
	}

	public Instant getCheckedAt() {
		return checkedAt;
	}
}
