package io.github.lelena1.travelbot.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Начальный белый список. При старте приложения эти ID записываются в базу;
 * дальше администратор может добавлять пользователей командой /allow.
 *
 * @param admins Telegram ID администраторов
 * @param users  Telegram ID обычных пользователей
 */
@ConfigurationProperties(prefix = "access")
public record AccessProperties(List<Long> admins, List<Long> users) {

	public AccessProperties {
		admins = admins == null ? List.of() : List.copyOf(admins);
		users = users == null ? List.of() : List.copyOf(users);
	}
}
