package io.github.lelena1.travelbot.lodging;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.lelena1.travelbot.bot.BotReply;
import io.github.lelena1.travelbot.bot.Keyboard;
import io.github.lelena1.travelbot.trip.Budget;
import io.github.lelena1.travelbot.trip.Pets;
import io.github.lelena1.travelbot.trip.Trip;

/** С настоящим справочником городов и всеми сайтами. */
class LodgingServiceTest {

	private final LodgingService service = new LodgingService(new LodgingCityDirectory(),
			List.of(new OstrovokSite(), new SutochnoSite(), new AvitoSite(), new CianSite()));

	@Test
	void russianCityGetsAllSitesAndHints() {
		BotReply reply = service.forTrip(trip("Казань", List.of(5, 8), Pets.DOG, Budget.UP_TO_6000));

		assertThat(reply.text())
				.startsWith("🏨 Жильё: Казань\n🗓 12 мая – 18 мая 2027 (6 ночей)\n👥 2 взрослых, 2 ребёнка (5, 8 лет)\n🐾 С собакой\n💰 до 6 000 ₽ за ночь")
				.contains("• Островок — даты, гости уже выбраны")
				.contains("• Суточно.ру — даты, гости, «с питомцами» уже выбраны")
				.contains("• Авито — посуточная аренда в городе")
				.contains("🐾 Островок, Авито, ЦИАН: отметьте «можно с животными»")
				.contains("Бюджет укажите в фильтре цены");
		assertThat(siteButtons(reply)).containsExactly("🔎 Островок", "🔎 Суточно.ру", "🔎 Авито", "🔎 ЦИАН");
	}

	@Test
	void foreignCityGetsOnlyOstrovok() {
		BotReply reply = service.forTrip(trip("Италия, Рим", List.of(), Pets.NONE, Budget.ANY));

		assertThat(reply.text()).startsWith("🏨 Жильё: Рим").doesNotContain("🐾").doesNotContain("💰");
		assertThat(siteButtons(reply)).containsExactly("🔎 Островок");
	}

	@Test
	void unknownCityFallsBackToSutochnoByName() {
		BotReply reply = service.forTrip(trip("Великие Луки", List.of(), Pets.NONE, Budget.ANY));

		assertThat(reply.text()).contains("Города «Великие Луки» нет в моём справочнике");
		assertThat(siteButtons(reply)).containsExactly("🔎 Суточно.ру");
	}

	private static List<String> siteButtons(BotReply reply) {
		return ((Keyboard.Links) reply.keyboard()).rows().stream().map(row -> row.get(0).text()).toList();
	}

	private static Trip trip(String destination, List<Integer> childAges, Pets pets, Budget budget) {
		Trip trip = Trip.draftFor(1);
		trip.setDestination(destination);
		trip.setOrigin("Москва");
		trip.setDates(LocalDate.of(2027, 5, 12), LocalDate.of(2027, 5, 18));
		trip.setAdults(2);
		trip.setChildren(childAges.size());
		trip.setChildAges(childAges);
		trip.setPets(pets);
		trip.setBudget(budget);
		return trip;
	}
}
