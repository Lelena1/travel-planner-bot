package io.github.lelena1.travelbot.lodging;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import io.github.lelena1.travelbot.lodging.LodgingLink.Filter;

/**
 * Суточно.ру: квартиры и дома посуточно. Город передаётся названием — сайт находит его сам,
 * поэтому ссылка работает и для городов, которых нет в справочнике.
 * Формат проверен на сайте: guests_childrens=5,8&amp;pets=1 → «4 гостя, с питомцами».
 */
@Component
@Order(2)
public class SutochnoSite implements LodgingSite {

	@Override
	public Optional<LodgingLink> link(LodgingRequest request) {
		boolean known = request.city().map(LodgingCity::sutochno).orElse(true);
		if (!known) {
			return Optional.empty();
		}
		StringBuilder url = new StringBuilder("https://sutochno.ru/front/searchapp/search?term=")
				.append(URLEncoder.encode(request.cityText(), StandardCharsets.UTF_8).replace("+", "%20"))
				.append("&occupied=").append(request.checkIn()).append(';').append(request.checkOut())
				.append("&guests_adults=").append(request.adults());
		if (!request.childAges().isEmpty()) {
			url.append("&guests_childrens=")
					.append(request.childAges().stream().map(String::valueOf).collect(Collectors.joining(",")));
		}
		EnumSet<Filter> applied = EnumSet.of(Filter.DATES, Filter.GUESTS);
		if (request.pets().any()) {
			url.append("&pets=1");
			applied.add(Filter.PETS);
		}
		return Optional.of(new LodgingLink("Суточно.ру", url.toString(), applied));
	}
}
