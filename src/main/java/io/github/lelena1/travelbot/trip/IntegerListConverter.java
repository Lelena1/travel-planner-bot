package io.github.lelena1.travelbot.trip;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Хранит список чисел (возраст детей) в одной колонке: [5, 8] ↔ "5,8". */
@Converter
public class IntegerListConverter implements AttributeConverter<List<Integer>, String> {

	@Override
	public String convertToDatabaseColumn(List<Integer> values) {
		if (values == null || values.isEmpty()) {
			return null;
		}
		return values.stream().map(String::valueOf).collect(Collectors.joining(","));
	}

	@Override
	public List<Integer> convertToEntityAttribute(String column) {
		if (column == null || column.isBlank()) {
			return List.of();
		}
		return Arrays.stream(column.split(",")).map(String::trim).map(Integer::valueOf).toList();
	}
}
