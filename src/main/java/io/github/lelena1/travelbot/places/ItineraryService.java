package io.github.lelena1.travelbot.places;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.github.lelena1.travelbot.bot.BotReply;
import io.github.lelena1.travelbot.bot.Keyboard.Link;
import io.github.lelena1.travelbot.city.GeoPoint;
import io.github.lelena1.travelbot.integration.osm.OverpassClient.OverpassException;
import io.github.lelena1.travelbot.integration.wiki.WikiException;
import io.github.lelena1.travelbot.places.ItineraryPlanner.DayPlan;
import io.github.lelena1.travelbot.trip.Trip;
import io.github.lelena1.travelbot.trip.TripFormatter;
import io.github.lelena1.travelbot.util.RussianPlural;

/** «🗓 План»: поездка по дням — что посмотреть, как дойти, где пообедать. */
@Service
public class ItineraryService {

	private static final Logger log = LoggerFactory.getLogger(ItineraryService.class);
	private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EE, d MMMM", Locale.forLanguageTag("ru"));

	/** Длиннее недели план по дням теряет смысл — дальше удобнее смотреть «🏛 Места». */
	static final int MAX_DAYS = 7;
	/** Дальше этого пешком уже не очень: предлагаем транспорт. */
	static final int WALKING_LIMIT_METERS = 2_500;
	/** Обед ищем не дальше этого от мест дня. */
	static final int LUNCH_RADIUS_METERS = 1_000;

	private final CityLocator locator;
	private final SightsService sights;
	private final RestaurantsService restaurants;
	private final ItineraryPlanner planner;

	public ItineraryService(CityLocator locator, SightsService sights, RestaurantsService restaurants,
			ItineraryPlanner planner) {
		this.locator = locator;
		this.sights = sights;
		this.restaurants = restaurants;
		this.planner = planner;
	}

	public BotReply forTrip(Trip trip) {
		String city = locator.cityName(trip.getDestination());
		Optional<GeoPoint> center;
		List<Sight> places;
		try {
			center = locator.locate(city);
			if (center.isEmpty()) {
				return BotReply.withMenu("Не нашла город «%s» в Википедии. Проверьте название в поездке.".formatted(city));
			}
			places = sights.topSights(city, center.get());
		} catch (WikiException e) {
			log.warn("План: Википедия недоступна: {}", e.getMessage());
			return BotReply.withMenu("😔 Википедия сейчас не отвечает. Попробуйте чуть позже.");
		}
		if (places.isEmpty()) {
			return BotReply.withMenu("🗓 Не нашлось известных мест в «%s», чтобы составить план. Загляните в «🎟 Экскурсии».".formatted(city));
		}

		int tripDays = (int) trip.nights() + 1;
		int days = Math.min(tripDays, MAX_DAYS);
		List<DayPlan> plan = planner.plan(places, days);
		List<Restaurant> lunchOptions = lunchOptions(city, center.get());
		log.info("План: {} — {} дн., мест {}, вариантов обеда {}", city, days, places.size(), lunchOptions.size());

		StringBuilder text = new StringBuilder("🗓 План поездки: ").append(city).append('\n')
				.append(TripFormatter.summaryDates(trip)).append(" · ")
				.append(RussianPlural.format(tripDays, "день", "дня", "дней")).append('\n');
		if (tripDays > MAX_DAYS) {
			text.append("План — на первые ").append(MAX_DAYS).append(" дней, остальное время — по настроению.\n");
		}

		List<List<Link>> buttons = new ArrayList<>();
		Set<String> usedLunch = new HashSet<>();
		for (DayPlan day : plan) {
			LocalDate date = trip.getDateFrom().plusDays(day.number() - 1L);
			text.append("\nДень ").append(day.number()).append(" · ").append(date.format(DAY)).append('\n');
			if (day.isFree()) {
				text.append("🌿 Свободный день: прогулки, музеи по интересам или поездка за город.\n");
				continue;
			}
			List<Integer> walks = day.walks();
			for (int i = 0; i < day.stops().size(); i++) {
				if (i > 0) {
					text.append("   ").append(walk(walks.get(i - 1))).append('\n');
				}
				text.append("🏛 ").append(day.stops().get(i).title()).append('\n');
			}
			nearestLunch(day, lunchOptions, usedLunch).ifPresent(place -> text.append("🍽 Обед рядом: ")
					.append(place.name()).append(place.cuisine() == null ? "" : " — " + place.cuisine()).append('\n'));
			buttons.add(List.of(new Link("🗺 Маршрут дня " + day.number(), routeUrl(day))));
		}
		text.append("\nМеста — самые известные в городе (по Википедии), сгруппированы по близости. ")
				.append("Подробности — в «🏛 Места», экскурсии — в «🎟 Экскурсии».");
		return BotReply.withLinks(text.toString(), buttons);
	}

	/** Рестораны для обеда; если OpenStreetMap не отвечает, план всё равно строится — просто без обедов. */
	private List<Restaurant> lunchOptions(String city, GeoPoint center) {
		try {
			return restaurants.top(city, center);
		} catch (OverpassException e) {
			log.warn("План: рестораны недоступны, план без обедов: {}", e.getMessage());
			return List.of();
		}
	}

	static Optional<Restaurant> nearestLunch(DayPlan day, List<Restaurant> options, Set<String> used) {
		GeoPoint centre = day.centre();
		Optional<Restaurant> best = options.stream()
				.filter(place -> !used.contains(place.name()))
				.filter(place -> place.point().distanceTo(centre) <= LUNCH_RADIUS_METERS)
				.min(Comparator.comparingDouble(place -> place.point().distanceTo(centre)));
		best.ifPresent(place -> used.add(place.name()));
		return best;
	}

	/** «🚶 650 м», «🚶 1,2 км» или «🚕 4,5 км — удобнее на транспорте». */
	static String walk(int meters) {
		String distance = meters < 1_000 ? (meters / 10 * 10) + " м"
				: String.format(Locale.forLanguageTag("ru"), "%.1f км", meters / 1000.0);
		return meters <= WALKING_LIMIT_METERS ? "🚶 " + distance : "🚕 " + distance + " — удобнее на транспорте";
	}

	static String routeUrl(DayPlan day) {
		if (day.stops().size() == 1) {
			return day.stops().get(0).point().yandexMapsUrl();
		}
		return GeoPoint.yandexWalkingRouteUrl(day.stops().stream().map(Sight::point).toList());
	}
}
