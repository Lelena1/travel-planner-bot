package io.github.lelena1.travelbot.trip;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class IntegerListConverterTest {

	private final IntegerListConverter converter = new IntegerListConverter();

	@Test
	void roundTrip() {
		String column = converter.convertToDatabaseColumn(List.of(5, 8));

		assertThat(column).isEqualTo("5,8");
		assertThat(converter.convertToEntityAttribute(column)).containsExactly(5, 8);
	}

	@Test
	void emptyListIsStoredAsNull() {
		assertThat(converter.convertToDatabaseColumn(List.of())).isNull();
		assertThat(converter.convertToEntityAttribute(null)).isEmpty();
	}
}
