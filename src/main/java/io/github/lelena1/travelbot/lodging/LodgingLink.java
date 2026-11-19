package io.github.lelena1.travelbot.lodging;

import java.util.Set;

/**
 * Ссылка на поиск жилья.
 *
 * @param site    название сайта для кнопки
 * @param url     адрес поиска
 * @param applied какие условия поездки уже подставлены в поиск; остальные пользователь выставит на сайте
 */
public record LodgingLink(String site, String url, Set<Filter> applied) {

	public LodgingLink {
		applied = Set.copyOf(applied);
	}

	public enum Filter {
		DATES, GUESTS, PETS
	}
}
