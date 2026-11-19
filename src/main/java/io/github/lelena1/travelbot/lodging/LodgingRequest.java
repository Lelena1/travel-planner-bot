package io.github.lelena1.travelbot.lodging;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import io.github.lelena1.travelbot.trip.Budget;
import io.github.lelena1.travelbot.trip.Pets;
import io.github.lelena1.travelbot.trip.Trip;
import io.github.lelena1.travelbot.util.CityNames;

/**
 * Что искать: город, даты и состав группы.
 *
 * @param cityText  город, как он написан в поездке (последняя часть: «Италия, Рим» → «Рим»)
 * @param city      город из справочника; пусто — города в справочнике нет
 * @param childAges возраст детей
 */
public record LodgingRequest(String cityText, Optional<LodgingCity> city, LocalDate checkIn, LocalDate checkOut,
		int adults, List<Integer> childAges, Pets pets, Budget budget) {

	public LodgingRequest {
		childAges = List.copyOf(childAges);
	}

	public static LodgingRequest of(Trip trip, Optional<LodgingCity> city) {
		List<String> parts = CityNames.partsFromEnd(trip.getDestination());
		String cityText = city.map(LodgingCity::name).orElse(parts.isEmpty() ? trip.getDestination() : parts.get(0));
		return new LodgingRequest(cityText, city, trip.getDateFrom(), trip.getDateTo(),
				trip.getAdults() == null ? 1 : trip.getAdults(), trip.getChildAges(),
				trip.getPets() == null ? Pets.NONE : trip.getPets(),
				trip.getBudget() == null ? Budget.ANY : trip.getBudget());
	}

	public int guests() {
		return adults + childAges.size();
	}
}
