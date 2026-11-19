package io.github.lelena1.travelbot.integration.travelpayouts;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Найденный вариант перелёта. Цены в Data API — из недавних поисков пользователей Aviasales
 * (кеш за последние дни), поэтому точную цену показывает сайт по ссылке.
 *
 * @param price           цена за одного взрослого, ₽
 * @param transfers       пересадок туда
 * @param returnTransfers пересадок обратно
 * @param durationTo      время в пути туда, минуты
 * @param durationBack    время в пути обратно, минуты
 * @param link            путь на сайте Aviasales (без домена), ведёт сразу на этот вариант
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PriceOffer(
		String origin,
		String destination,
		@JsonProperty("origin_airport") String originAirport,
		@JsonProperty("destination_airport") String destinationAirport,
		// время — местное для аэропорта вылета; не переводим в UTC, иначе «20:50» станет «17:50»
		@JsonProperty("departure_at") @JsonFormat(without = JsonFormat.Feature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE) OffsetDateTime departureAt,
		@JsonProperty("return_at") @JsonFormat(without = JsonFormat.Feature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE) OffsetDateTime returnAt,
		String airline,
		@JsonProperty("flight_number") String flightNumber,
		int price,
		int transfers,
		@JsonProperty("return_transfers") int returnTransfers,
		@JsonProperty("duration_to") int durationTo,
		@JsonProperty("duration_back") int durationBack,
		String link) {

	public boolean isRoundTrip() {
		return returnAt != null;
	}

	public boolean isDirect() {
		return transfers == 0 && (!isRoundTrip() || returnTransfers == 0);
	}
}
