package io.github.lelena1.travelbot.city;

import java.util.List;
import java.util.Optional;

/**
 * Город из справочника cities.yml: как его называют сайты с жильём и экскурсиями.
 *
 * @param ostrovok путь на ostrovok.ru («russia/kazan»)
 * @param avito    город в адресе avito.ru («kazan»); пусто — сайт не используется
 * @param cian     поддомен cian.ru («kazan», у Москвы «www»); пусто — сайт не используется
 * @param sutochno есть ли город на Суточно.ру
 * @param tripster город в адресе experience.tripster.ru («Kazan»); пусто — экскурсий на Трипстере нет
 */
public record KnownCity(String name, List<String> aliases, String ostrovok, String avito, String cian,
		boolean sutochno, String tripster) {

	public KnownCity {
		aliases = aliases == null ? List.of() : List.copyOf(aliases);
	}

	public Optional<String> ostrovokPath() {
		return Optional.ofNullable(ostrovok);
	}

	public Optional<String> avitoCity() {
		return Optional.ofNullable(avito);
	}

	public Optional<String> cianSubdomain() {
		return Optional.ofNullable(cian);
	}

	public Optional<String> tripsterCity() {
		return Optional.ofNullable(tripster);
	}
}
