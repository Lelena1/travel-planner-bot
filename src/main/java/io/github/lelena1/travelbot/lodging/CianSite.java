package io.github.lelena1.travelbot.lodging;

import java.util.Optional;
import java.util.Set;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** ЦИАН: посуточная аренда квартир в городе; фильтры выставляются на сайте. */
@Component
@Order(4)
public class CianSite implements LodgingSite {

	@Override
	public Optional<LodgingLink> link(LodgingRequest request) {
		return request.city().flatMap(LodgingCity::cianSubdomain).map(subdomain -> new LodgingLink("ЦИАН",
				"https://%s.cian.ru/snyat-kvartiru-posutochno/".formatted(subdomain), Set.of()));
	}
}
