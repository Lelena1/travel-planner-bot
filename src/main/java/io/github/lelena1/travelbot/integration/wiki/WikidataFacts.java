package io.github.lelena1.travelbot.integration.wiki;

import java.util.Set;

/**
 * Что известно об объекте из Викиданных.
 *
 * @param sitelinks  в скольких Википедиях (и других проектах) есть статья — мера известности
 * @param instanceOf чем объект является (свойство P31): например, Q33506 — музей
 * @param properties какие свойства заполнены: P1082 — население, P585 — дата события…
 */
public record WikidataFacts(int sitelinks, Set<String> instanceOf, Set<String> properties) {

	public WikidataFacts {
		instanceOf = Set.copyOf(instanceOf);
		properties = Set.copyOf(properties);
	}

	/** Есть население — значит, это город, район или регион, а не достопримечательность. */
	public boolean hasPopulation() {
		return properties.contains("P1082");
	}

	/** Есть дата события или период — значит, это событие (битва, операция), а не место. */
	public boolean isEvent() {
		return properties.contains("P585") || (properties.contains("P580") && properties.contains("P582"));
	}
}
