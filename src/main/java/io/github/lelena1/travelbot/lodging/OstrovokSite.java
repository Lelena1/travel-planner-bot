package io.github.lelena1.travelbot.lodging;

import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import io.github.lelena1.travelbot.city.KnownCity;
import io.github.lelena1.travelbot.lodging.LodgingLink.Filter;

/**
 * Островок: отели и апартаменты в России и за рубежом.
 * Формат проверен на сайте: ?dates=12.05.2027-18.05.2027&amp;guests=2and5.8 —
 * «1 номер для 4 гостей»: двое взрослых и дети 5 и 8 лет.
 */
@Component
@Order(1)
public class OstrovokSite implements LodgingSite {

	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

	@Override
	public Optional<LodgingLink> link(LodgingRequest request) {
		return request.city().flatMap(KnownCity::ostrovokPath).map(path -> new LodgingLink("Островок",
				"https://ostrovok.ru/hotel/%s/?search=yes&dates=%s-%s&guests=%s".formatted(path,
						request.checkIn().format(DATE), request.checkOut().format(DATE), guests(request)),
				EnumSet.of(Filter.DATES, Filter.GUESTS)));
	}

	/** «2» — двое взрослых, «2and5.8» — и дети 5 и 8 лет. */
	static String guests(LodgingRequest request) {
		if (request.childAges().isEmpty()) {
			return String.valueOf(request.adults());
		}
		return request.adults() + "and"
				+ request.childAges().stream().map(String::valueOf).collect(Collectors.joining("."));
	}
}
