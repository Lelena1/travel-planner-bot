package io.github.lelena1.travelbot.pricewatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.github.lelena1.travelbot.TestcontainersConfiguration;
import io.github.lelena1.travelbot.bot.BotReply;
import io.github.lelena1.travelbot.bot.Keyboard;
import io.github.lelena1.travelbot.flights.FlightSearchService;
import io.github.lelena1.travelbot.flights.Passengers;
import io.github.lelena1.travelbot.integration.travelpayouts.Place;
import io.github.lelena1.travelbot.integration.travelpayouts.PriceOffer;
import io.github.lelena1.travelbot.trip.Trip;
import io.github.lelena1.travelbot.trip.TripRepository;
import io.github.lelena1.travelbot.user.AccessService;

/** Подписки на цены на настоящей PostgreSQL; поиск цен подменён. Без Docker тест пропускается. */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PriceWatchServiceIntegrationTest {

	static final Place MOSCOW = new Place("MOW", "Москва", "city", "Россия", null);
	static final Place ROME = new Place("ROM", "Рим", "city", "Италия", null);
	private static final AtomicLong NEXT_USER = new AtomicLong(30_000);

	@MockitoBean
	private FlightSearchService flights;

	@Autowired
	private PriceWatchService service;
	@Autowired
	private PriceWatchRepository watches;
	@Autowired
	private PriceSnapshotRepository snapshots;
	@Autowired
	private TripRepository trips;
	@Autowired
	private AccessService access;

	private long user;
	private Trip trip;

	@BeforeEach
	void setUp() {
		user = NEXT_USER.incrementAndGet();
		access.allow(user);
		trip = saveTrip(user, LocalDate.now().plusMonths(6));
	}

	@Test
	void subscribeRemembersCurrentPrice() {
		when(flights.search(any())).thenReturn(found(trip, true, 47_948));

		BotReply reply = service.subscribe(user, user, trip.getId());

		assertThat(reply.text()).contains("Слежу за ценой: Москва → Рим").contains("от 47 948 ₽");
		PriceWatch watch = watches.findByTripId(trip.getId()).orElseThrow();
		assertThat(watch.getOriginCode()).isEqualTo("MOW");
		assertThat(watch.getDestinationCode()).isEqualTo("ROM");
		assertThat(watch.getLastPrice()).isEqualTo(47_948);
		assertThat(watch.getMinPrice()).isEqualTo(47_948);
		assertThat(snapshots.findByWatchIdOrderByCheckedAtAsc(watch.getId()))
				.extracting(PriceSnapshot::getPrice).containsExactly(47_948);
	}

	@Test
	void secondSubscriptionForSameTripIsNotCreated() {
		when(flights.search(any())).thenReturn(found(trip, true, 47_948));
		service.subscribe(user, user, trip.getId());

		assertThat(service.subscribe(user, user, trip.getId()).text()).contains("уже слежу");
		assertThat(watches.countByUserId(user)).isEqualTo(1);
	}

	@Test
	void withoutPricesOnTripDatesWatchStartsEmpty() {
		when(flights.search(any())).thenReturn(found(trip, false, 21_000));

		assertThat(service.subscribe(user, user, trip.getId()).text()).contains("Цен на даты поездки пока нет");
		assertThat(watches.findByTripId(trip.getId()).orElseThrow().getLastPrice()).isNull();
	}

	@Test
	void cannotSubscribeToSomeoneElsesTrip() {
		long stranger = NEXT_USER.incrementAndGet();
		access.allow(stranger);

		assertThat(service.subscribe(stranger, stranger, trip.getId()).text()).contains("не найдена");
		verify(flights, never()).search(any());
	}

	@Test
	void pastTripCannotBeWatched() {
		Trip past = saveTrip(user, LocalDate.now().minusDays(1));

		assertThat(service.subscribe(user, user, past.getId()).text()).contains("уже началась");
	}

	@Test
	void searchProblemIsShownAndNothingSaved() {
		when(flights.search(any())).thenReturn(new FlightSearchService.Problem("Не нашла аэропорт"));

		assertThat(service.subscribe(user, user, trip.getId()).text()).isEqualTo("Не нашла аэропорт");
		assertThat(watches.findByTripId(trip.getId())).isEmpty();
	}

	@Test
	void listShowsWatchesWithUnsubscribeButtons() {
		when(flights.search(any())).thenReturn(found(trip, true, 47_948));
		service.subscribe(user, user, trip.getId());
		long watchId = watches.findByTripId(trip.getId()).orElseThrow().getId();

		BotReply reply = service.list(user);

		assertThat(reply.text()).contains("1. Москва → Рим").contains("Сейчас от 47 948 ₽").contains("проверено");
		assertThat(((Keyboard.Links) reply.keyboard()).rows().get(0).get(0).callbackData())
				.isEqualTo("unwatch:" + watchId);
	}

	@Test
	void emptyListExplainsHowToSubscribe() {
		assertThat(service.list(user).text()).contains("Подписок на цены пока нет");
	}

	@Test
	void unsubscribeOnlyOwnWatch() {
		when(flights.search(any())).thenReturn(found(trip, true, 47_948));
		service.subscribe(user, user, trip.getId());
		long watchId = watches.findByTripId(trip.getId()).orElseThrow().getId();

		assertThat(service.unsubscribe(user + 1, watchId).text()).contains("уже нет");
		assertThat(service.unsubscribe(user, watchId).text()).contains("Больше не слежу");
		assertThat(watches.findById(watchId)).isEmpty();
	}

	@Test
	void deletingTripRemovesItsWatch() {
		when(flights.search(any())).thenReturn(found(trip, true, 47_948));
		service.subscribe(user, user, trip.getId());

		trips.deleteById(trip.getId());

		assertThat(watches.findByTripId(trip.getId())).isEmpty();
	}

	private Trip saveTrip(long userId, LocalDate from) {
		Trip saved = Trip.draftFor(userId);
		saved.setOrigin("Москва");
		saved.setDestination("Рим");
		saved.setDates(from, from.plusDays(6));
		saved.setAdults(2);
		saved.setChildren(0);
		saved.activate();
		return trips.save(saved);
	}

	static FlightSearchService.Found found(Trip trip, boolean exactDates, int price) {
		PriceOffer offer = new PriceOffer("MOW", "ROM", "SVO", "FCO", OffsetDateTime.parse("2027-05-12T20:50:00+03:00"),
				OffsetDateTime.parse("2027-05-18T11:55:00+02:00"), "SU", "2400", price, 0, 0, 230, 240,
				"/search/MOW1205ROM18051?t=abc");
		return new FlightSearchService.Found(MOSCOW, ROME, trip, Passengers.of(trip), List.of(offer), exactDates,
				"https://www.aviasales.ru/search/MOW1205ROM18052");
	}
}
