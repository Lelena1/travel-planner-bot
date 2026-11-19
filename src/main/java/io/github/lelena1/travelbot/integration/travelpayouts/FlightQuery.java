package io.github.lelena1.travelbot.integration.travelpayouts;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Что искать: откуда, куда и когда.
 *
 * @param origin      IATA-код города вылета (MOW)
 * @param destination IATA-код города назначения (ROM)
 * @param departureAt дата «2027-05-12» или месяц «2027-05»
 * @param returnAt    дата/месяц обратного вылета; null — билет в одну сторону
 */
public record FlightQuery(String origin, String destination, String departureAt, String returnAt) {

	/** Туда и обратно в конкретные даты. */
	public static FlightQuery roundTrip(String origin, String destination, LocalDate there, LocalDate back) {
		return new FlightQuery(origin, destination, there.toString(), back.toString());
	}

	/** Самый дешёвый вылет в течение месяца (когда на точные даты ничего нет). */
	public static FlightQuery anyDayIn(String origin, String destination, YearMonth month) {
		return new FlightQuery(origin, destination, month.toString(), null);
	}

	public boolean oneWay() {
		return returnAt == null;
	}
}
