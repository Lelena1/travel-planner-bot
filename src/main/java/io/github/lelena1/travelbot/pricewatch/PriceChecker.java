package io.github.lelena1.travelbot.pricewatch;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.github.lelena1.travelbot.bot.BotReply;
import io.github.lelena1.travelbot.bot.Keyboard.Link;
import io.github.lelena1.travelbot.bot.Notifier;
import io.github.lelena1.travelbot.flights.FlightFormatter;
import io.github.lelena1.travelbot.flights.FlightSearchService;
import io.github.lelena1.travelbot.integration.travelpayouts.PriceOffer;
import io.github.lelena1.travelbot.trip.Trip;
import io.github.lelena1.travelbot.trip.TripFormatter;
import io.github.lelena1.travelbot.trip.TripRepository;

/**
 * Проверяет цены по всем подпискам и сообщает о снижении.
 * Запускается по расписанию ({@link PriceCheckJob}) или вручную командой администратора /checkprices.
 */
@Service
public class PriceChecker {

	private static final Logger log = LoggerFactory.getLogger(PriceChecker.class);

	private final PriceWatchRepository watches;
	private final PriceSnapshotRepository snapshots;
	private final TripRepository trips;
	private final FlightSearchService flights;
	private final Notifier notifier;
	private final Clock clock;

	public PriceChecker(PriceWatchRepository watches, PriceSnapshotRepository snapshots, TripRepository trips,
			FlightSearchService flights, Notifier notifier, Clock clock) {
		this.watches = watches;
		this.snapshots = snapshots;
		this.trips = trips;
		this.flights = flights;
		this.notifier = notifier;
		this.clock = clock;
	}

	public Summary checkAll() {
		List<PriceWatch> all = watches.findAll();
		int notified = 0;
		int failed = 0;
		int finished = 0;
		for (PriceWatch watch : all) {
			try {
				switch (check(watch)) {
					case NOTIFIED -> notified++;
					case FINISHED -> finished++;
					case FAILED -> failed++;
					case UNCHANGED -> {
					}
				}
			} catch (RuntimeException e) {
				// одна сломанная подписка не должна останавливать проверку остальных
				failed++;
				log.error("Цены: ошибка при проверке подписки #{}", watch.getId(), e);
			}
		}
		Summary summary = new Summary(all.size(), notified, finished, failed);
		log.info("Цены: проверено {}, уведомлений {}, завершено {}, ошибок {}", summary.checked(),
				summary.notified(), summary.finished(), summary.failed());
		return summary;
	}

	Outcome check(PriceWatch watch) {
		Optional<Trip> trip = trips.findById(watch.getTripId());
		if (trip.isEmpty() || trip.get().getDateFrom().isBefore(LocalDate.now(clock))) {
			watches.delete(watch);
			log.info("Цены: подписка #{} завершена — поездка удалена или уже началась", watch.getId());
			return Outcome.FINISHED;
		}

		FlightSearchService.Result result = flights.search(trip.get());
		if (result instanceof FlightSearchService.Problem problem) {
			log.warn("Цены: подписка #{} не проверена: {}", watch.getId(), problem.message());
			return Outcome.FAILED;
		}
		FlightSearchService.Found found = (FlightSearchService.Found) result;
		Integer previous = watch.getLastPrice();
		Integer previousMin = watch.getMinPrice();
		Integer current = PriceWatchService.cheapestOnTripDates(found);

		watch.recordPrice(current, clock.instant());
		watches.save(watch);
		snapshots.save(new PriceSnapshot(watch.getId(), current, clock.instant()));

		PriceChange change = PriceChange.of(previous, current);
		if (change == PriceChange.NONE) {
			return Outcome.UNCHANGED;
		}
		notifier.send(watch.getChatId(), alert(watch, trip.get(), found, change, previous, previousMin,
				flights::offerUrl));
		log.info("Цены: подписка #{} {} → {}, отправлено уведомление ({})", watch.getId(), previous, current, change);
		return Outcome.NOTIFIED;
	}

	static BotReply alert(PriceWatch watch, Trip trip, FlightSearchService.Found found, PriceChange change,
			Integer previous, Integer previousMin, Function<PriceOffer, String> offerUrl) {
		int current = watch.getLastPrice();
		StringBuilder text = new StringBuilder();
		if (change == PriceChange.DROPPED) {
			text.append("📉 Билеты подешевели!\n");
		} else {
			text.append("🆕 Появились цены на даты поездки!\n");
		}
		text.append("✈️ ").append(watch.getRouteTitle()).append('\n');
		text.append("🗓 ").append(TripFormatter.summaryDates(trip)).append("\n\n");
		text.append("💰 ").append(FlightFormatter.price(current)).append(" ₽ за взрослого, туда и обратно");
		if (change == PriceChange.DROPPED) {
			text.append("\n   было ").append(FlightFormatter.price(previous)).append(" ₽ (−")
					.append(PriceChange.dropPercent(previous, current)).append("%)");
			if (previousMin == null || current < previousMin) {
				text.append("\n   🏆 это самая низкая цена за всё время слежения");
			}
		}
		// уведомление бывает только при найденной цене, значит, самый дешёвый вариант есть
		Link buy = new Link("Купить за %s ₽".formatted(FlightFormatter.price(current)),
				offerUrl.apply(found.offers().get(0)));
		return BotReply.withLinks(text.toString(), List.of(
				List.of(buy),
				List.of(new Link("🔎 Все варианты на Aviasales", found.searchUrl())),
				List.of(Link.action("🔕 Больше не следить", PriceWatchService.UNWATCH_ACTION + watch.getId()))));
	}

	enum Outcome {
		NOTIFIED, UNCHANGED, FINISHED, FAILED
	}

	/** Итог проверки — для журнала и для ответа на /checkprices. */
	public record Summary(int checked, int notified, int finished, int failed) {
	}
}
