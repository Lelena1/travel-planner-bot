package io.github.lelena1.travelbot.pricewatch;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Подписка на цену билетов для одной поездки. */
@Entity
@Table(name = "price_watch")
public class PriceWatch {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	/** Куда присылать уведомления (в личном чате совпадает с user_id). */
	@Column(name = "chat_id", nullable = false)
	private Long chatId;

	@Column(name = "trip_id", nullable = false)
	private Long tripId;

	@Column(name = "origin_code", nullable = false, length = 3)
	private String originCode;

	@Column(name = "destination_code", nullable = false, length = 3)
	private String destinationCode;

	/** «Москва → Рим», чтобы не искать названия городов при каждой проверке. */
	@Column(name = "route_title", nullable = false)
	private String routeTitle;

	/** Цена при последней проверке, ₽ за взрослого; null — цен на даты поездки пока нет. */
	@Column(name = "last_price")
	private Integer lastPrice;

	/** Самая низкая цена за всё время слежения. */
	@Column(name = "min_price")
	private Integer minPrice;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "last_checked_at")
	private Instant lastCheckedAt;

	protected PriceWatch() {
		// для JPA
	}

	public PriceWatch(long userId, long chatId, long tripId, String originCode, String destinationCode,
			String routeTitle, Integer price, Instant now) {
		this.userId = userId;
		this.chatId = chatId;
		this.tripId = tripId;
		this.originCode = originCode;
		this.destinationCode = destinationCode;
		this.routeTitle = routeTitle;
		this.createdAt = now;
		recordPrice(price, now);
	}

	/** Запоминает результат проверки; минимальная цена обновляется сама. */
	public void recordPrice(Integer price, Instant now) {
		this.lastPrice = price;
		this.lastCheckedAt = now;
		if (price != null && (minPrice == null || price < minPrice)) {
			this.minPrice = price;
		}
	}

	public Long getId() {
		return id;
	}

	public Long getUserId() {
		return userId;
	}

	public Long getChatId() {
		return chatId;
	}

	public Long getTripId() {
		return tripId;
	}

	public String getOriginCode() {
		return originCode;
	}

	public String getDestinationCode() {
		return destinationCode;
	}

	public String getRouteTitle() {
		return routeTitle;
	}

	public Integer getLastPrice() {
		return lastPrice;
	}

	public Integer getMinPrice() {
		return minPrice;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getLastCheckedAt() {
		return lastCheckedAt;
	}
}
