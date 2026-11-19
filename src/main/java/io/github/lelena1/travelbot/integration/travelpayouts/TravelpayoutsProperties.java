package io.github.lelena1.travelbot.integration.travelpayouts;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Настройки Travelpayouts (данные Aviasales).
 *
 * @param token           API-токен из личного кабинета Travelpayouts
 * @param apiUrl          адрес Data API с ценами
 * @param autocompleteUrl адрес поиска городов (название → IATA-код)
 * @param aviasalesUrl    сайт, на который ведут ссылки «Купить»
 */
@ConfigurationProperties(prefix = "travelpayouts")
public record TravelpayoutsProperties(
		String token,
		@DefaultValue("https://api.travelpayouts.com") String apiUrl,
		@DefaultValue("https://autocomplete.travelpayouts.com") String autocompleteUrl,
		@DefaultValue("https://www.aviasales.ru") String aviasalesUrl) {
}
