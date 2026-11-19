package io.github.lelena1.travelbot.lodging;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.github.lelena1.travelbot.bot.BotReply;
import io.github.lelena1.travelbot.bot.Keyboard.Link;
import io.github.lelena1.travelbot.lodging.LodgingLink.Filter;
import io.github.lelena1.travelbot.trip.Budget;
import io.github.lelena1.travelbot.trip.Trip;
import io.github.lelena1.travelbot.trip.TripFormatter;

/**
 * Поиск жилья для поездки: ссылки на сайты с уже подставленными условиями
 * и подсказки, что выставить на сайте вручную.
 */
@Service
public class LodgingService {

	private static final Logger log = LoggerFactory.getLogger(LodgingService.class);

	private final LodgingCityDirectory directory;
	private final List<LodgingSite> sites;

	/** Сайты приходят в порядке {@code @Order}: сначала те, где больше фильтров в ссылке. */
	public LodgingService(LodgingCityDirectory directory, List<LodgingSite> sites) {
		this.directory = directory;
		this.sites = List.copyOf(sites);
	}

	public BotReply forTrip(Trip trip) {
		Optional<LodgingCity> city = directory.find(trip.getDestination());
		LodgingRequest request = LodgingRequest.of(trip, city);
		List<LodgingLink> links = sites.stream()
				.map(site -> site.link(request))
				.flatMap(Optional::stream)
				.toList();
		log.info("Жильё: {} — {}", request.cityText(),
				links.stream().map(LodgingLink::site).collect(Collectors.joining(", ")));

		StringBuilder text = new StringBuilder("🏨 Жильё: ").append(request.cityText()).append('\n')
				.append("🗓 ").append(TripFormatter.summaryDates(trip)).append('\n')
				.append("👥 ").append(TripFormatter.travelers(trip));
		if (request.pets().any()) {
			text.append("\n🐾 ").append(TripFormatter.pets(request.pets()));
		}
		if (request.budget() != Budget.ANY) {
			text.append("\n💰 до ").append(request.budget().label().replace("до ", "")).append(" за ночь");
		}
		text.append("\n\n");

		if (city.isEmpty()) {
			text.append("ℹ️ Города «").append(request.cityText())
					.append("» нет в моём справочнике, поэтому ищу только на Суточно.ру — по названию.\n\n");
		}
		text.append("Поиск с вашими условиями:\n");
		for (LodgingLink link : links) {
			text.append("• ").append(link.site()).append(" — ").append(describeApplied(link)).append('\n');
		}
		appendHints(text, request, links);

		List<List<Link>> buttons = links.stream()
				.map(link -> List.of(new Link("🔎 " + link.site(), link.url())))
				.toList();
		return BotReply.withLinks(text.toString().stripTrailing(), buttons);
	}

	static String describeApplied(LodgingLink link) {
		if (link.applied().isEmpty()) {
			return "посуточная аренда в городе, даты и гостей выберите на сайте";
		}
		List<String> parts = new ArrayList<>();
		if (link.applied().contains(Filter.DATES)) {
			parts.add("даты");
		}
		if (link.applied().contains(Filter.GUESTS)) {
			parts.add("гости");
		}
		if (link.applied().contains(Filter.PETS)) {
			parts.add("«с питомцами»");
		}
		return String.join(", ", parts) + " уже выбраны";
	}

	private static void appendHints(StringBuilder text, LodgingRequest request, List<LodgingLink> links) {
		if (request.pets().any()) {
			String withoutPets = links.stream()
					.filter(link -> !link.applied().contains(Filter.PETS))
					.map(LodgingLink::site)
					.collect(Collectors.joining(", "));
			if (!withoutPets.isEmpty()) {
				text.append("\n🐾 ").append(withoutPets)
						.append(": отметьте «можно с животными» в фильтрах или уточните у хозяина/отеля.");
			}
		}
		if (request.budget() != Budget.ANY) {
			text.append("\n💰 Бюджет укажите в фильтре цены на сайте — в ссылку он не передаётся.");
		}
	}
}
