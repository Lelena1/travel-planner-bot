package io.github.lelena1.travelbot.pricewatch;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.github.lelena1.travelbot.bot.BotReply;
import io.github.lelena1.travelbot.bot.Keyboard.Link;
import io.github.lelena1.travelbot.flights.FlightFormatter;
import io.github.lelena1.travelbot.flights.FlightSearchService;
import io.github.lelena1.travelbot.trip.Trip;
import io.github.lelena1.travelbot.trip.TripFormatter;
import io.github.lelena1.travelbot.trip.TripRepository;
import io.github.lelena1.travelbot.trip.TripStatus;

/** Подписки на цену билетов: подписаться, отписаться, посмотреть свои подписки. */
@Service
public class PriceWatchService {

	private static final Logger log = LoggerFactory.getLogger(PriceWatchService.class);
	private static final DateTimeFormatter CHECKED_AT = DateTimeFormatter.ofPattern("d MMMM, HH:mm",
			Locale.forLanguageTag("ru"));
	private static final ZoneId MOSCOW = ZoneId.of("Europe/Moscow");

	/** Действие кнопки «Следить за ценой»: «watch:<id поездки>» (кнопку рисует {@link FlightFormatter}). */
	public static final String WATCH_ACTION = FlightFormatter.WATCH_ACTION;
	/** Действие кнопки «Отписаться»: «unwatch:<id подписки>». */
	public static final String UNWATCH_ACTION = "unwatch:";

	static final int MAX_WATCHES_PER_USER = 10;

	private final PriceWatchRepository watches;
	private final PriceSnapshotRepository snapshots;
	private final TripRepository trips;
	private final FlightSearchService flights;
	private final Clock clock;

	public PriceWatchService(PriceWatchRepository watches, PriceSnapshotRepository snapshots, TripRepository trips,
			FlightSearchService flights, Clock clock) {
		this.watches = watches;
		this.snapshots = snapshots;
		this.trips = trips;
		this.flights = flights;
		this.clock = clock;
	}

	public BotReply subscribe(long userId, long chatId, long tripId) {
		Optional<Trip> found = trips.findById(tripId)
				.filter(trip -> trip.getUserId() == userId && trip.getStatus() == TripStatus.ACTIVE);
		if (found.isEmpty()) {
			return BotReply.withMenu("Поездка не найдена — возможно, её удалили.");
		}
		Trip trip = found.get();
		if (trip.getDateFrom().isBefore(LocalDate.now(clock))) {
			return BotReply.withMenu("Поездка уже началась — следить за ценой поздно.");
		}
		Optional<PriceWatch> existing = watches.findByTripId(tripId);
		if (existing.isPresent()) {
			return BotReply.withMenu("Я уже слежу за ценой этой поездки. %s\nВсе подписки — «🔔 Мои подписки»."
					.formatted(currentPrice(existing.get().getLastPrice())));
		}
		if (watches.countByUserId(userId) >= MAX_WATCHES_PER_USER) {
			return BotReply.withMenu("Можно следить не больше чем за %d поездками. Отпишитесь от ненужных в «🔔 Мои подписки»."
					.formatted(MAX_WATCHES_PER_USER));
		}

		FlightSearchService.Result result = flights.search(trip);
		if (result instanceof FlightSearchService.Problem problem) {
			return BotReply.withMenu(problem.message());
		}
		FlightSearchService.Found prices = (FlightSearchService.Found) result;
		Integer price = cheapestOnTripDates(prices);
		PriceWatch watch = watches.save(new PriceWatch(userId, chatId, tripId, prices.from().code(),
				prices.to().code(), prices.from().name() + " → " + prices.to().name(), price, clock.instant()));
		snapshots.save(new PriceSnapshot(watch.getId(), price, clock.instant()));
		log.info("Цены: пользователь {} подписался на {} (поездка #{}, цена {})", userId, watch.getRouteTitle(),
				tripId, price);

		return BotReply.withMenu("""
				🔔 Слежу за ценой: %s
				🗓 %s
				%s

				Проверяю цены несколько раз в день и напишу, когда билеты подешевеют."""
				.formatted(watch.getRouteTitle(), TripFormatter.summaryDates(trip), currentPrice(price)));
	}

	public BotReply unsubscribe(long userId, long watchId) {
		Optional<PriceWatch> watch = watches.findById(watchId).filter(w -> w.getUserId() == userId);
		if (watch.isEmpty()) {
			return BotReply.withMenu("Такой подписки уже нет.");
		}
		watches.delete(watch.get());
		log.info("Цены: пользователь {} отписался от {} (подписка #{})", userId, watch.get().getRouteTitle(), watchId);
		return BotReply.withMenu("🔕 Больше не слежу за ценой: " + watch.get().getRouteTitle());
	}

	/** Раздел «🔔 Мои подписки». */
	public BotReply list(long userId) {
		List<PriceWatch> mine = watches.findByUserIdOrderByCreatedAtAsc(userId);
		if (mine.isEmpty()) {
			return BotReply.withMenu("""
					🔔 Подписок на цены пока нет.
					Откройте «✈️ Авиабилеты» и нажмите «🔔 Следить за ценой» под найденными билетами.""");
		}
		StringBuilder text = new StringBuilder("🔔 Слежу за ценами:\n");
		List<List<Link>> buttons = new ArrayList<>();
		for (int i = 0; i < mine.size(); i++) {
			PriceWatch watch = mine.get(i);
			text.append('\n').append(i + 1).append(". ").append(watch.getRouteTitle());
			trips.findById(watch.getTripId())
					.ifPresent(trip -> text.append("\n   🗓 ").append(TripFormatter.summaryDates(trip)));
			text.append("\n   ").append(currentPrice(watch.getLastPrice()));
			if (watch.getMinPrice() != null && !watch.getMinPrice().equals(watch.getLastPrice())) {
				text.append(" · минимум был ").append(FlightFormatter.price(watch.getMinPrice())).append(" ₽");
			}
			if (watch.getLastCheckedAt() != null) {
				text.append("\n   проверено ").append(CHECKED_AT.format(watch.getLastCheckedAt().atZone(MOSCOW)));
			}
			text.append('\n');
			buttons.add(List.of(Link.action("🔕 Отписаться: %d. %s".formatted(i + 1, watch.getRouteTitle()),
					UNWATCH_ACTION + watch.getId())));
		}
		return BotReply.withLinks(text.toString(), buttons);
	}

	static Integer cheapestOnTripDates(FlightSearchService.Found prices) {
		return prices.exactDates() && !prices.offers().isEmpty() ? prices.offers().get(0).price() : null;
	}

	static String currentPrice(Integer price) {
		return price == null
				? "💰 Цен на даты поездки пока нет — напишу, когда появятся."
				: "💰 Сейчас от " + FlightFormatter.price(price) + " ₽ за взрослого (туда и обратно).";
	}
}
