package io.github.lelena1.travelbot.flights;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import io.github.lelena1.travelbot.bot.BotReply;
import io.github.lelena1.travelbot.bot.Keyboard.Link;
import io.github.lelena1.travelbot.integration.travelpayouts.PriceOffer;
import io.github.lelena1.travelbot.trip.TripFormatter;
import io.github.lelena1.travelbot.util.RussianPlural;

/** Сообщение с найденными билетами и кнопками «Купить». */
public final class FlightFormatter {

	private static final Locale RU = Locale.forLanguageTag("ru");
	private static final DateTimeFormatter DEPARTURE = DateTimeFormatter.ofPattern("d MMMM, HH:mm", RU);
	private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("LLLL yyyy", RU);

	private FlightFormatter() {
	}

	public static BotReply toReply(FlightSearchService.Result result, Function<PriceOffer, String> offerUrl) {
		if (result instanceof FlightSearchService.Problem problem) {
			return BotReply.withMenu(problem.message());
		}
		FlightSearchService.Found found = (FlightSearchService.Found) result;
		StringBuilder text = new StringBuilder();
		text.append("✈️ ").append(found.from().name()).append(" → ").append(found.to().name()).append('\n');
		text.append("🗓 ").append(TripFormatter.summaryDates(found.trip())).append('\n');
		text.append("👥 ").append(RussianPlural.format(found.passengers().total(), "пассажир", "пассажира", "пассажиров"))
				.append("\n\n");

		Link searchAll = new Link("🔎 Все варианты на Aviasales", found.searchUrl());
		if (found.offers().isEmpty()) {
			text.append("В базе Aviasales пока нет цен на это направление в эти даты. ")
					.append("Откройте поиск — сайт найдёт актуальные билеты.");
			return BotReply.withLinks(text.toString(), List.of(List.of(searchAll)));
		}

		if (found.exactDates()) {
			text.append("Самые дешёвые билеты туда и обратно на даты поездки (цена за 1 взрослого):\n");
		} else {
			text.append("На точные даты цен в базе пока нет. Самые дешёвые билеты в одну сторону, ")
					.append(found.trip().getDateFrom().format(MONTH))
					.append(" (цена за 1 взрослого):\n");
		}

		List<List<Link>> buttons = new ArrayList<>();
		List<PriceOffer> offers = found.offers();
		for (int i = 0; i < offers.size(); i++) {
			PriceOffer offer = offers.get(i);
			text.append('\n').append(i + 1).append(". ").append(describe(offer)).append('\n');
			buttons.add(List.of(new Link("%d. Купить за %s ₽".formatted(i + 1, price(offer.price())),
					offerUrl.apply(offer))));
		}
		buttons.add(List.of(searchAll));

		int cheapest = offers.get(0).price();
		if (found.passengers().total() > 1) {
			text.append("\n💡 На всех ориентировочно от ")
					.append(price((long) cheapest * found.passengers().total()))
					.append(" ₽ (детские билеты обычно дешевле).");
		}
		text.append("\nℹ️ Цены — из недавних поисков на Aviasales, точную цену покажет сайт.");
		return BotReply.withLinks(text.toString(), buttons);
	}

	/** «47 948 ₽ · 12 мая, 20:50 · 1 пересадка, 7 ч 55 мин / обратно 18 мая, 11:55 · 1 пересадка». */
	static String describe(PriceOffer offer) {
		StringBuilder line = new StringBuilder();
		line.append(price(offer.price())).append(" ₽\n   туда: ").append(leg(offer.departureAt(), offer.transfers(),
				offer.durationTo()));
		if (offer.isRoundTrip()) {
			line.append("\n   обратно: ").append(leg(offer.returnAt(), offer.returnTransfers(), offer.durationBack()));
		}
		return line.toString();
	}

	private static String leg(OffsetDateTime departure, int transfers, int durationMinutes) {
		String result = departure.format(DEPARTURE) + " · " + transfers(transfers);
		if (durationMinutes > 0) {
			result += ", " + duration(durationMinutes);
		}
		return result;
	}

	static String transfers(int count) {
		return count == 0 ? "прямой" : RussianPlural.format(count, "пересадка", "пересадки", "пересадок");
	}

	static String duration(int minutes) {
		int hours = minutes / 60;
		int rest = minutes % 60;
		if (hours == 0) {
			return rest + " мин";
		}
		return rest == 0 ? hours + " ч" : hours + " ч " + rest + " мин";
	}

	/** 47948 → «47 948». */
	public static String price(long rubles) {
		return String.format(Locale.ROOT, "%,d", rubles).replace(',', ' ');
	}
}
