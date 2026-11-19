package io.github.lelena1.travelbot.city;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CityDirectoryTest {

	/** Настоящий файл cities.yml. */
	private final CityDirectory directory = new CityDirectory();

	@ParameterizedTest(name = "«{0}» → {1}")
	@CsvSource(delimiter = '|', value = {
			"Казань                 | Казань",
			"казань                 | Казань",
			"Италия, Рим            | Рим",
			"Рим, Италия            | Рим",
			"Питер                  | Санкт-Петербург",
			"санкт петербург        | Санкт-Петербург",
			"Россия, Ростов-на-Дону | Ростов-на-Дону",
			"Нижний                 | Нижний Новгород",
			"Алма-Ата               | Алматы"
	})
	void findsCityByNameOrAlias(String destination, String expected) {
		assertThat(directory.find(destination)).get().extracting(KnownCity::name).isEqualTo(expected);
	}

	@Test
	void unknownCity() {
		assertThat(directory.find("Хогвартс")).isEmpty();
		assertThat(directory.find(null)).isEmpty();
	}

	@Test
	void fileIsLoadedCompletely() {
		assertThat(directory.size()).isGreaterThanOrEqualTo(40);
		KnownCity kazan = directory.find("Казань").orElseThrow();
		assertThat(kazan.ostrovokPath()).contains("russia/kazan");
		assertThat(kazan.avitoCity()).contains("kazan");
		assertThat(kazan.cianSubdomain()).contains("kazan");
		assertThat(kazan.sutochno()).isTrue();
		assertThat(kazan.tripsterCity()).contains("Kazan");
		assertThat(directory.find("Калининград").orElseThrow().nearby())
				.containsExactly("Зеленоградск", "Светлогорск (Калининградская область)", "Куршская коса | Танцующий лес");
		assertThat(directory.find("Новосибирск").orElseThrow().nearby()).isEmpty();
		assertThat(directory.find("Сириус").orElseThrow().tripsterCity()).isEmpty();
	}

	@Test
	void foreignCitiesHaveNoRussianListingSites() {
		KnownCity rome = directory.find("Рим").orElseThrow();

		assertThat(rome.ostrovokPath()).contains("italy/rome");
		assertThat(rome.avitoCity()).isEmpty();
		assertThat(rome.cianSubdomain()).isEmpty();
	}

	@Test
	void everyCityHasOstrovokPath() {
		assertThat(CityDirectory.load(CityDirectory.RESOURCE))
				.allSatisfy(city -> assertThat(city.ostrovokPath()).as(city.name()).isPresent());
	}

	@Test
	void duplicateNamesAreRejected() {
		KnownCity one = new KnownCity("Сочи", List.of(), "russia/sochi", null, null, true, null, null);
		KnownCity two = new KnownCity("Адлер", List.of("Сочи"), "russia/adler", null, null, true, null, null);

		assertThatThrownBy(() -> new CityDirectory(List.of(one, two)))
				.hasMessageContaining("встречается у двух городов");
	}
}
