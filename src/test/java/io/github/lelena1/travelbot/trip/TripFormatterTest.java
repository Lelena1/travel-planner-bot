package io.github.lelena1.travelbot.trip;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class TripFormatterTest {

	@Test
	void fullSummary() {
		Trip trip = Trip.draftFor(1);
		trip.setDestination("Италия, Рим");
		trip.setOrigin("Москва");
		trip.setDates(LocalDate.of(2027, 5, 12), LocalDate.of(2027, 5, 18));
		trip.setAdults(2);
		trip.setChildren(2);
		trip.setChildAges(List.of(5, 8));
		trip.setPets(Pets.DOG);
		trip.setBudget(Budget.UP_TO_6000);

		assertThat(TripFormatter.summary(trip)).isEqualTo("""
				📍 Италия, Рим
				🛫 Откуда: Москва
				🗓 12 мая – 18 мая 2027 (6 ночей)
				👥 2 взрослых, 2 ребёнка (5, 8 лет)
				🐾 С собакой
				💰 Жильё до 6 000 ₽ за ночь""");
	}

	@Test
	void summaryOfDraftShowsOnlyFilledFields() {
		Trip trip = Trip.draftFor(1);
		trip.setDestination("Казань");

		assertThat(TripFormatter.summary(trip)).isEqualTo("📍 Казань");
	}

	@Test
	void datesAcrossNewYearShowBothYears() {
		assertThat(TripFormatter.dates(LocalDate.of(2026, 12, 28), LocalDate.of(2027, 1, 5)))
				.isEqualTo("28 декабря 2026 – 5 января 2027");
	}

	@Test
	void oneAdultOneChild() {
		Trip trip = Trip.draftFor(1);
		trip.setAdults(1);
		trip.setChildren(1);
		trip.setChildAges(List.of(1));

		assertThat(TripFormatter.travelers(trip)).isEqualTo("1 взрослый, 1 ребёнок (1 год)");
	}

	@Test
	void noChildrenAreNotMentioned() {
		Trip trip = Trip.draftFor(1);
		trip.setAdults(3);
		trip.setChildren(0);

		assertThat(TripFormatter.travelers(trip)).isEqualTo("3 взрослых");
	}
}
