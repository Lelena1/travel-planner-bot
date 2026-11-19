package io.github.lelena1.travelbot.city;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import io.github.lelena1.travelbot.util.CityNames;

/**
 * Справочник популярных городов (src/main/resources/cities.yml): как их называют сайты с жильём и экскурсиями.
 * Данные отделены от кода: чтобы добавить город, достаточно дописать его в файл.
 */
@Component
public class CityDirectory {

	static final String RESOURCE = "cities.yml";

	private final Map<String, KnownCity> byName = new HashMap<>();

	public CityDirectory() {
		this(load(RESOURCE));
	}

	CityDirectory(List<KnownCity> cities) {
		for (KnownCity city : cities) {
			register(city.name(), city);
			city.aliases().forEach(alias -> register(alias, city));
		}
	}

	/**
	 * Ищет город в том, что написано в поездке: «Италия, Рим» → Рим, «Питер» → Санкт-Петербург.
	 * Части через запятую проверяются с конца.
	 */
	public Optional<KnownCity> find(String destination) {
		if (destination == null) {
			return Optional.empty();
		}
		return CityNames.partsFromEnd(destination).stream()
				.map(part -> byName.get(CityNames.normalize(part)))
				.filter(city -> city != null)
				.findFirst();
	}

	public int size() {
		return (int) byName.values().stream().distinct().count();
	}

	private void register(String name, KnownCity city) {
		KnownCity previous = byName.put(CityNames.normalize(name), city);
		if (previous != null && previous != city) {
			throw new IllegalStateException("Название «%s» встречается у двух городов: %s и %s"
					.formatted(name, previous.name(), city.name()));
		}
	}

	@SuppressWarnings("unchecked")
	static List<KnownCity> load(String resource) {
		try (InputStream in = new ClassPathResource(resource).getInputStream()) {
			Map<String, Object> root = new Yaml().load(in);
			List<Map<String, Object>> cities = (List<Map<String, Object>>) root.get("cities");
			return cities.stream()
					.map(city -> new KnownCity(
							(String) city.get("name"),
							(List<String>) city.get("aliases"),
							(String) city.get("ostrovok"),
							(String) city.get("avito"),
							(String) city.get("cian"),
							Boolean.TRUE.equals(city.get("sutochno"))))
					.toList();
		} catch (IOException e) {
			throw new UncheckedIOException("Не удалось прочитать " + resource, e);
		}
	}
}
