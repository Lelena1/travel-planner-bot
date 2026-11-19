package io.github.lelena1.travelbot.pricewatch;

import static io.github.lelena1.travelbot.pricewatch.PriceWatchServiceIntegrationTest.found;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.github.lelena1.travelbot.TestcontainersConfiguration;
import io.github.lelena1.travelbot.bot.BotReply;
import io.github.lelena1.travelbot.bot.Keyboard;
import io.github.lelena1.travelbot.bot.Notifier;
import io.github.lelena1.travelbot.flights.FlightSearchService;
import io.github.lelena1.travelbot.trip.Trip;
import io.github.lelena1.travelbot.trip.TripRepository;
import io.github.lelena1.travelbot.user.AccessService;

/** Проверка цен на настоящей PostgreSQL; поиск цен и отправка в Telegram подменены. */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PriceCheckerIntegrationTest {

	private static final AtomicLong NEXT_USER = new AtomicLong(40_000);

	@MockitoBean
	private FlightSearchService flights;
	@MockitoBean
	private Notifier notifier;

	@Autowired
	private PriceChecker checker;
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
		watches.deleteAll();
		user = NEXT_USER.incrementAndGet();
		access.allow(user);
		trip = saveTrip(LocalDate.now().plusMonths(6));
		when(flights.offerUrl(any())).thenReturn("https://www.aviasales.ru/search/offer");
	}

	@Test
	void priceDropSendsAlertWithBuyAndUnsubscribeButtons() {
		subscribeAt(47_948);
		when(flights.search(any())).thenReturn(found(trip, true, 45_000));

		PriceChecker.Summary summary = checker.checkAll();

		assertThat(summary).isEqualTo(new PriceChecker.Summary(1, 1, 0, 0));
		ArgumentCaptor<BotReply> alert = ArgumentCaptor.forClass(BotReply.class);
		verify(notifier).send(eq(user), alert.capture());
		assertThat(alert.getValue().text())
				.startsWith("📉 Билеты подешевели!")
				.contains("45 000 ₽").contains("было 47 948 ₽ (−6%)")
				.contains("самая низкая цена");
		Keyboard.Links buttons = (Keyboard.Links) alert.getValue().keyboard();
		assertThat(buttons.rows().get(0).get(0).url()).isEqualTo("https://www.aviasales.ru/search/offer");
		assertThat(buttons.rows().get(2).get(0).callbackData()).startsWith("unwatch:");

		PriceWatch watch = watches.findByTripId(trip.getId()).orElseThrow();
		assertThat(watch.getLastPrice()).isEqualTo(45_000);
		assertThat(watch.getMinPrice()).isEqualTo(45_000);
		assertThat(snapshots.findByWatchIdOrderByCheckedAtAsc(watch.getId()))
				.extracting(PriceSnapshot::getPrice).containsExactly(47_948, 45_000);
	}

	@Test
	void smallChangeIsRecordedSilently() {
		subscribeAt(47_948);
		when(flights.search(any())).thenReturn(found(trip, true, 47_500));

		assertThat(checker.checkAll()).isEqualTo(new PriceChecker.Summary(1, 0, 0, 0));

		verify(notifier, never()).send(anyLong(), any());
		assertThat(watches.findByTripId(trip.getId()).orElseThrow().getLastPrice()).isEqualTo(47_500);
	}

	@Test
	void appearedPriceIsReported() {
		when(flights.search(any())).thenReturn(found(trip, false, 21_000));
		service.subscribe(user, user, trip.getId());
		when(flights.search(any())).thenReturn(found(trip, true, 47_948));

		checker.checkAll();

		ArgumentCaptor<BotReply> alert = ArgumentCaptor.forClass(BotReply.class);
		verify(notifier).send(eq(user), alert.capture());
		assertThat(alert.getValue().text()).startsWith("🆕 Появились цены");
	}

	@Test
	void startedTripFinishesWatch() {
		subscribeAt(47_948);
		Trip started = trips.findById(trip.getId()).orElseThrow();
		started.setDates(LocalDate.now().minusDays(1), LocalDate.now().plusDays(3));
		trips.save(started);

		assertThat(checker.checkAll()).isEqualTo(new PriceChecker.Summary(1, 0, 1, 0));
		assertThat(watches.findByTripId(trip.getId())).isEmpty();
	}

	@Test
	void unavailableServiceKeepsOldPrice() {
		subscribeAt(47_948);
		when(flights.search(any())).thenReturn(new FlightSearchService.Problem("не отвечает"));

		assertThat(checker.checkAll()).isEqualTo(new PriceChecker.Summary(1, 0, 0, 1));
		assertThat(watches.findByTripId(trip.getId()).orElseThrow().getLastPrice()).isEqualTo(47_948);
	}

	private void subscribeAt(int price) {
		when(flights.search(any())).thenReturn(found(trip, true, price));
		service.subscribe(user, user, trip.getId());
	}

	private Trip saveTrip(LocalDate from) {
		Trip saved = Trip.draftFor(user);
		saved.setOrigin("Москва");
		saved.setDestination("Рим");
		saved.setDates(from, from.plusDays(6));
		saved.setAdults(2);
		saved.setChildren(0);
		saved.activate();
		return trips.save(saved);
	}
}
