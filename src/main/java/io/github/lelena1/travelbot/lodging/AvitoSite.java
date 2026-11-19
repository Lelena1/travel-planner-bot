package io.github.lelena1.travelbot.lodging;

import java.util.Optional;
import java.util.Set;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import io.github.lelena1.travelbot.city.KnownCity;

/**
 * Авито: посуточная аренда квартир в городе. Даты, гостей и «можно с животными»
 * Авито хранит в закодированных фильтрах, поэтому они выставляются на сайте.
 */
@Component
@Order(3)
public class AvitoSite implements LodgingSite {

	@Override
	public Optional<LodgingLink> link(LodgingRequest request) {
		return request.city().flatMap(KnownCity::avitoCity).map(city -> new LodgingLink("Авито",
				"https://www.avito.ru/%s/kvartiry/sdam/posutochno-ASgBAgICAkSSA8gQ8AeSUg".formatted(city), Set.of()));
	}
}
