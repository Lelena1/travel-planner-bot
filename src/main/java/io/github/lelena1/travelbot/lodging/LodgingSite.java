package io.github.lelena1.travelbot.lodging;

import java.util.Optional;

/** Сайт с жильём: умеет построить ссылку на поиск с условиями поездки. */
public interface LodgingSite {

	/** Пусто, если для этого города сайт не подходит. */
	Optional<LodgingLink> link(LodgingRequest request);
}
