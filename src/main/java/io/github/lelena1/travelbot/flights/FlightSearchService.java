package io.github.lelena1.travelbot.flights;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.github.lelena1.travelbot.integration.travelpayouts.AviasalesClient;
import io.github.lelena1.travelbot.integration.travelpayouts.FlightQuery;
import io.github.lelena1.travelbot.integration.travelpayouts.Place;
import io.github.lelena1.travelbot.integration.travelpayouts.PriceOffer;
import io.github.lelena1.travelbot.integration.travelpayouts.TravelpayoutsException;
import io.github.lelena1.travelbot.integration.travelpayouts.TravelpayoutsProperties;
import io.github.lelena1.travelbot.trip.Trip;

/** Ищет самые дешёвые авиабилеты для сохранённой поездки. */
@Service
public class FlightSearchService {

	private static final Logger log = LoggerFactory.getLogger(FlightSearchService.class);
	private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("ddMM");

	static final int OFFERS_LIMIT = 5;

	private final CityResolver cities;
	private final AviasalesClient aviasales;
	private final TravelpayoutsProperties properties;

	public FlightSearchService(CityResolver cities, AviasalesClient aviasales, TravelpayoutsProperties properties) {
		this.cities = cities;
		this.aviasales = aviasales;
		this.properties = properties;
	}

	public Result search(Trip trip) {
		try {
			Place from = city(trip.getOrigin(), "откуда");
			Place to = city(trip.getDestination(), "куда");
			if (from.code().equals(to.code())) {
				return new Problem("Город вылета и назначения совпадают (%s). Создайте поездку заново."
						.formatted(from.name()));
			}

			Passengers passengers = Passengers.of(trip);
			String searchUrl = searchUrl(from, to, trip.getDateFrom(), trip.getDateTo(), passengers);

			List<PriceOffer> exact = aviasales.cheapest(
					FlightQuery.roundTrip(from.code(), to.code(), trip.getDateFrom(), trip.getDateTo()), OFFERS_LIMIT);
			if (!exact.isEmpty()) {
				log.info("Билеты: {} → {} на даты поездки — {} вариантов", from.code(), to.code(), exact.size());
				return new Found(from, to, trip, passengers, exact, true, searchUrl);
			}

			YearMonth month = YearMonth.from(trip.getDateFrom());
			List<PriceOffer> monthly = aviasales.cheapest(FlightQuery.anyDayIn(from.code(), to.code(), month),
					OFFERS_LIMIT);
			log.info("Билеты: {} → {} на даты поездки нет, в {} — {} вариантов", from.code(), to.code(), month,
					monthly.size());
			return new Found(from, to, trip, passengers, monthly, false, searchUrl);
		} catch (CityProblem e) {
			return new Problem(e.getMessage());
		} catch (TravelpayoutsException e) {
			log.warn("Билеты: сервис Travelpayouts недоступен: {}", e.getMessage());
			return new Problem("😔 Сервис цен Aviasales сейчас не отвечает. Попробуйте чуть позже.");
		}
	}

	/** Ссылка на поиск Aviasales сразу с датами и пассажирами: /search/MOW1205ROM18052. */
	String searchUrl(Place from, Place to, LocalDate there, LocalDate back, Passengers passengers) {
		return properties.aviasalesUrl() + "/search/" + from.code() + there.format(DAY_MONTH) + to.code()
				+ back.format(DAY_MONTH) + passengers.aviasalesSuffix();
	}

	public String offerUrl(PriceOffer offer) {
		return aviasales.offerUrl(offer);
	}

	private Place city(String text, String role) {
		if (text == null || text.isBlank()) {
			throw new CityProblem("В поездке не указано, " + role + " лететь. Создайте поездку заново.");
		}
		CityResolver.Result result = cities.resolve(text);
		if (result instanceof CityResolver.Found found) {
			return found.city();
		}
		if (result instanceof CityResolver.CountryOnly country) {
			throw new CityProblem("«%s» — это страна, а билеты продаются до конкретного города. Создайте поездку и укажите город, например: %s, Рим"
					.formatted(country.country(), country.country()));
		}
		throw new CityProblem("Не нашла аэропорт для «%s». Проверьте название города и создайте поездку заново."
				.formatted(text));
	}

	private static final class CityProblem extends RuntimeException {
		CityProblem(String message) {
			super(message);
		}
	}

	public sealed interface Result permits Found, Problem {
	}

	/**
	 * @param exactDates true — варианты на даты поездки; false — самые дешёвые вылеты в том же месяце
	 * @param searchUrl  поиск на Aviasales с датами поездки и всеми пассажирами
	 */
	public record Found(Place from, Place to, Trip trip, Passengers passengers, List<PriceOffer> offers,
			boolean exactDates, String searchUrl) implements Result {
	}

	public record Problem(String message) implements Result {
	}
}
