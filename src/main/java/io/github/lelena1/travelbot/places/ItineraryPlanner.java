package io.github.lelena1.travelbot.places;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.springframework.stereotype.Component;

import io.github.lelena1.travelbot.city.GeoPoint;

/**
 * Раскладывает достопримечательности по дням.
 * <p>
 * Жадная кластеризация: день начинается с самого известного ещё не распределённого места,
 * к нему добавляются ближайшие к «центру дня» места. Внутри дня порядок обхода — «к ближайшему
 * следующему» (nearest neighbour), чтобы не ходить зигзагами. Это не оптимальный маршрут
 * коммивояжёра, но для 2–3 точек в день разница незаметна, а алгоритм прост и предсказуем.
 */
@Component
public class ItineraryPlanner {

	/** Больше трёх мест в день — уже не отдых, а марафон. */
	static final int MAX_PER_DAY = 3;

	public List<DayPlan> plan(List<Sight> sightsByFame, int days) {
		if (days < 1) {
			return List.of();
		}
		int total = Math.min(sightsByFame.size(), days * MAX_PER_DAY);
		int perDay = Math.max(1, Math.min(MAX_PER_DAY, (int) Math.ceil((double) total / days)));
		LinkedList<Sight> unassigned = new LinkedList<>(sightsByFame.subList(0, total));

		List<DayPlan> plan = new ArrayList<>();
		for (int day = 1; day <= days; day++) {
			List<Sight> stops = new ArrayList<>();
			if (!unassigned.isEmpty()) {
				stops.add(unassigned.removeFirst());   // самое известное из оставшихся
				while (stops.size() < perDay && !unassigned.isEmpty()) {
					GeoPoint centre = centre(stops);
					Sight nearest = unassigned.stream()
							.min(Comparator.comparingDouble(sight -> sight.point().distanceTo(centre)))
							.orElseThrow();
					unassigned.remove(nearest);
					stops.add(nearest);
				}
			}
			plan.add(new DayPlan(day, walkingOrder(stops)));
		}
		return plan;
	}

	/** Порядок обхода: от первого места каждый раз к ближайшему из оставшихся. */
	static List<Sight> walkingOrder(List<Sight> stops) {
		if (stops.size() < 3) {
			return List.copyOf(stops);
		}
		List<Sight> rest = new ArrayList<>(stops.subList(1, stops.size()));
		List<Sight> route = new ArrayList<>(List.of(stops.get(0)));
		while (!rest.isEmpty()) {
			GeoPoint here = route.get(route.size() - 1).point();
			Sight next = rest.stream()
					.min(Comparator.comparingDouble(sight -> sight.point().distanceTo(here)))
					.orElseThrow();
			rest.remove(next);
			route.add(next);
		}
		return route;
	}

	static GeoPoint centre(List<Sight> stops) {
		double lat = stops.stream().mapToDouble(sight -> sight.point().lat()).average().orElse(0);
		double lon = stops.stream().mapToDouble(sight -> sight.point().lon()).average().orElse(0);
		return new GeoPoint(lat, lon);
	}

	/**
	 * План одного дня.
	 *
	 * @param stops места в порядке обхода; пусто — свободный день
	 */
	public record DayPlan(int number, List<Sight> stops) {

		public DayPlan {
			stops = List.copyOf(stops);
		}

		public boolean isFree() {
			return stops.isEmpty();
		}

		/** Пешком между соседними местами, метры. */
		public List<Integer> walks() {
			List<Integer> walks = new ArrayList<>();
			for (int i = 1; i < stops.size(); i++) {
				walks.add((int) Math.round(stops.get(i - 1).point().distanceTo(stops.get(i).point())));
			}
			return walks;
		}

		public GeoPoint centre() {
			return ItineraryPlanner.centre(stops);
		}
	}
}
