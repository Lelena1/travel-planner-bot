package io.github.lelena1.travelbot.lodging;

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
 * Справочник городов для поиска жилья (src/main/resources/lodging-cities.yml).
 * Данные отделены от кода: чтобы добавить город, достаточно дописать его в файл.
 */
@Component
public class LodgingCityDirectory {

	static final String RESOURCE = "lodging-cities.yml";

	private final Map<String, LodgingCity> byName = new HashMap<>();

	public LodgingCityDirectory() {
		this(load(RESOURCE));
	}

	LodgingCityDirectory(List<LodgingCity> cities) {
		for (LodgingCity city : cities) {
			register(city.name(), city);
			city.aliases().forEach(alias -> register(alias, city));
		}
	}

	/**
	 * Ищет город в том, что написано в поездке: «Италия, Рим» → Рим, «Питер» → Санкт-Петербург.
	 * Части через запятую проверяются с конца.
	 */
	public Optional<LodgingCity> find(String destination) {
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

	private void register(String name, LodgingCity city) {
		LodgingCity previous = byName.put(CityNames.normalize(name), city);
		if (previous != null && previous != city) {
			throw new IllegalStateException("Название «%s» встречается у двух городов: %s и %s"
					.formatted(name, previous.name(), city.name()));
		}
	}

	@SuppressWarnings("unchecked")
	static List<LodgingCity> load(String resource) {
		try (InputStream in = new ClassPathResource(resource).getInputStream()) {
			Map<String, Object> root = new Yaml().load(in);
			List<Map<String, Object>> cities = (List<Map<String, Object>>) root.get("cities");
			return cities.stream()
					.map(city -> new LodgingCity(
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
