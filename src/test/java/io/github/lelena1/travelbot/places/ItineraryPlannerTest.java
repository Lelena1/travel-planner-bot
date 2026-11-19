package io.github.lelena1.travelbot.places;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.lelena1.travelbot.city.GeoPoint;
import io.github.lelena1.travelbot.places.ItineraryPlanner.DayPlan;

class ItineraryPlannerTest {

	private final ItineraryPlanner planner = new ItineraryPlanner();

	// два района: «центр» около (54.70, 20.50) и «север» около (54.75, 20.50), между ними ~5,5 км
	private static final Sight CATHEDRAL = sight("Собор", 54.700, 20.500, 40);
	private static final Sight ZOO = sight("Зоопарк", 54.750, 20.500, 30);
	private static final Sight CASTLE = sight("Замок", 54.702, 20.501, 25);
	private static final Sight PARK = sight("Парк", 54.751, 20.502, 20);
	private static final Sight GATE = sight("Ворота", 54.701, 20.498, 15);
	private static final Sight LAKE = sight("Озеро", 54.749, 20.497, 10);

	@Test
	void nearbyPlacesGoToTheSameDay() {
		List<DayPlan> plan = planner.plan(List.of(CATHEDRAL, ZOO, CASTLE, PARK, GATE, LAKE), 2);

		assertThat(plan).hasSize(2);
		assertThat(plan.get(0).stops()).containsExactlyInAnyOrder(CATHEDRAL, CASTLE, GATE);
		assertThat(plan.get(1).stops()).containsExactlyInAnyOrder(ZOO, PARK, LAKE);
		assertThat(plan.get(0).stops().get(0)).as("день начинается с самого известного").isEqualTo(CATHEDRAL);
	}

	@Test
	void walksBetweenStopsAreShort() {
		DayPlan day = planner.plan(List.of(CATHEDRAL, ZOO, CASTLE, PARK, GATE, LAKE), 2).get(0);

		assertThat(day.walks()).hasSize(2).allSatisfy(meters -> assertThat(meters).isLessThan(500));
	}

	@Test
	void atMostThreePlacesPerDayAndFreeDaysAtTheEnd() {
		List<DayPlan> plan = planner.plan(List.of(CATHEDRAL, ZOO, CASTLE, PARK, GATE, LAKE), 5);

		assertThat(plan).hasSize(5);
		assertThat(plan).allSatisfy(day -> assertThat(day.stops()).hasSizeLessThanOrEqualTo(ItineraryPlanner.MAX_PER_DAY));
		assertThat(plan.stream().mapToInt(day -> day.stops().size()).sum()).isEqualTo(6);
	}

	@Test
	void shortTripTakesOnlyTheMostFamous() {
		List<DayPlan> plan = planner.plan(List.of(CATHEDRAL, ZOO, CASTLE, PARK, GATE, LAKE), 1);

		assertThat(plan).singleElement().satisfies(day -> assertThat(day.stops()).hasSize(3).contains(CATHEDRAL));
	}

	@Test
	void moreDaysThanPlacesLeavesFreeDays() {
		List<DayPlan> plan = planner.plan(List.of(CATHEDRAL), 3);

		assertThat(plan).extracting(DayPlan::isFree).containsExactly(false, true, true);
	}

	@Test
	void walkingOrderAvoidsZigzag() {
		Sight a = sight("A", 54.700, 20.500, 1);
		Sight far = sight("Далеко", 54.700, 20.530, 1);
		Sight middle = sight("Середина", 54.700, 20.515, 1);

		assertThat(ItineraryPlanner.walkingOrder(List.of(a, far, middle))).containsExactly(a, middle, far);
	}

	private static Sight sight(String title, double lat, double lon, int fame) {
		return new Sight(title, null, null, new GeoPoint(lat, lon), "https://ru.wikipedia.org/wiki/" + title, fame);
	}
}
