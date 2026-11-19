package io.github.lelena1.travelbot.flights;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import io.github.lelena1.travelbot.bot.BotReply;
import io.github.lelena1.travelbot.bot.Keyboard;
import io.github.lelena1.travelbot.integration.travelpayouts.PriceOffer;
import io.github.lelena1.travelbot.trip.Trip;

class FlightFormatterTest {

	private static final String SEARCH_URL = "https://www.aviasales.ru/search/MOW1205ROM180521";

	@Test
	void roundTripOffersWithBuyButtons() {
		Trip trip = FlightSearchServiceTest.trip("Москва", "Италия, Рим");
		PriceOffer cheap = FlightSearchServiceTest.offer(47948);
		PriceOffer expensive = FlightSearchServiceTest.offer(52100);
		FlightSearchService.Found found = new FlightSearchService.Found(FlightSearchServiceTest.MOSCOW,
				FlightSearchServiceTest.ROME, trip, Passengers.of(trip), List.of(cheap, expensive), true, SEARCH_URL);

		BotReply reply = FlightFormatter.toReply(found, offer -> "https://www.aviasales.ru" + offer.link());

		assertThat(reply.text())
				.startsWith("✈️ Москва → Рим\n🗓 12 мая – 18 мая 2027 (6 ночей)\n👥 3 пассажира")
				.contains("туда и обратно на даты поездки")
				.contains("1. 47 948 ₽\n   туда: 12 мая, 20:50 · 1 пересадка, 7 ч 55 мин")
				.contains("обратно: 18 мая, 11:55 · прямой, 3 ч 50 мин")
				.contains("2. 52 100 ₽")
				.contains("На всех ориентировочно от 143 844 ₽");

		Keyboard.Links links = (Keyboard.Links) reply.keyboard();
		assertThat(links.rows()).hasSize(3);
		assertThat(links.rows().get(0).get(0))
				.isEqualTo(new Keyboard.Link("1. Купить за 47 948 ₽", "https://www.aviasales.ru/search/MOW1205ROM18051?t=abc"));
		assertThat(links.rows().get(2).get(0).url()).isEqualTo(SEARCH_URL);
	}

	@Test
	void monthFallbackIsExplained() {
		Trip trip = FlightSearchServiceTest.trip("Москва", "Рим");
		FlightSearchService.Found found = new FlightSearchService.Found(FlightSearchServiceTest.MOSCOW,
				FlightSearchServiceTest.ROME, trip, Passengers.of(trip),
				List.of(FlightSearchServiceTest.offer(21000)), false, SEARCH_URL);

		assertThat(FlightFormatter.toReply(found, offer -> "x").text())
				.contains("На точные даты цен в базе пока нет")
				.contains("май 2027");
	}

	@Test
	void noOffersStillGivesSearchLink() {
		Trip trip = FlightSearchServiceTest.trip("Москва", "Рим");
		FlightSearchService.Found found = new FlightSearchService.Found(FlightSearchServiceTest.MOSCOW,
				FlightSearchServiceTest.ROME, trip, Passengers.of(trip), List.of(), false, SEARCH_URL);

		BotReply reply = FlightFormatter.toReply(found, offer -> "x");

		assertThat(reply.text()).contains("пока нет цен");
		assertThat(((Keyboard.Links) reply.keyboard()).rows()).singleElement()
				.satisfies(row -> assertThat(row.get(0).url()).isEqualTo(SEARCH_URL));
	}

	@Test
	void problemIsShownWithMainMenu() {
		BotReply reply = FlightFormatter.toReply(new FlightSearchService.Problem("Не нашла аэропорт"), offer -> "x");

		assertThat(reply.text()).isEqualTo("Не нашла аэропорт");
		assertThat(reply.showsMainMenu()).isTrue();
	}

	@Test
	void helpers() {
		assertThat(FlightFormatter.price(1234567)).isEqualTo("1 234 567");
		assertThat(FlightFormatter.duration(45)).isEqualTo("45 мин");
		assertThat(FlightFormatter.duration(120)).isEqualTo("2 ч");
		assertThat(FlightFormatter.transfers(2)).isEqualTo("2 пересадки");
	}

	@Test
	void savedTripGetsWatchButton() {
		Trip trip = FlightSearchServiceTest.trip("Москва", "Рим");
		ReflectionTestUtils.setField(trip, "id", 7L);
		FlightSearchService.Found found = new FlightSearchService.Found(FlightSearchServiceTest.MOSCOW,
				FlightSearchServiceTest.ROME, trip, Passengers.of(trip),
				List.of(FlightSearchServiceTest.offer(47948)), true, SEARCH_URL);

		List<List<Keyboard.Link>> rows = ((Keyboard.Links) FlightFormatter.toReply(found, offer -> "x").keyboard()).rows();

		assertThat(rows.get(rows.size() - 1).get(0)).isEqualTo(Keyboard.Link.action("🔔 Следить за ценой", "watch:7"));
	}
}
