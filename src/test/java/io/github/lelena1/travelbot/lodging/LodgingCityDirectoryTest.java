package io.github.lelena1.travelbot.lodging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LodgingCityDirectoryTest {

	/** Настоящий файл lodging-cities.yml. */
	private final LodgingCityDirectory directory = new LodgingCityDirectory();

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
		assertThat(directory.find(destination)).get().extracting(LodgingCity::name).isEqualTo(expected);
	}

	@Test
	void unknownCity() {
		assertThat(directory.find("Хогвартс")).isEmpty();
		assertThat(directory.find(null)).isEmpty();
	}

	@Test
	void fileIsLoadedCompletely() {
		assertThat(directory.size()).isGreaterThanOrEqualTo(40);
		LodgingCity kazan = directory.find("Казань").orElseThrow();
		assertThat(kazan.ostrovokPath()).contains("russia/kazan");
		assertThat(kazan.avitoCity()).contains("kazan");
		assertThat(kazan.cianSubdomain()).contains("kazan");
		assertThat(kazan.sutochno()).isTrue();
	}

	@Test
	void foreignCitiesHaveNoRussianListingSites() {
		LodgingCity rome = directory.find("Рим").orElseThrow();

		assertThat(rome.ostrovokPath()).contains("italy/rome");
		assertThat(rome.avitoCity()).isEmpty();
		assertThat(rome.cianSubdomain()).isEmpty();
	}

	@Test
	void everyCityHasOstrovokPath() {
		assertThat(LodgingCityDirectory.load(LodgingCityDirectory.RESOURCE))
				.allSatisfy(city -> assertThat(city.ostrovokPath()).as(city.name()).isPresent());
	}

	@Test
	void duplicateNamesAreRejected() {
		LodgingCity one = new LodgingCity("Сочи", List.of(), "russia/sochi", null, null, true);
		LodgingCity two = new LodgingCity("Адлер", List.of("Сочи"), "russia/adler", null, null, true);

		assertThatThrownBy(() -> new LodgingCityDirectory(List.of(one, two)))
				.hasMessageContaining("встречается у двух городов");
	}
}
