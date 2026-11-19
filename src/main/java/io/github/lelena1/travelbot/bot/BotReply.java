package io.github.lelena1.travelbot.bot;

/**
 * Ответ бота.
 *
 * @param text         текст сообщения
 * @param showMainMenu показать ли под сообщением кнопки главного меню
 */
public record BotReply(String text, boolean showMainMenu) {

	public static BotReply text(String text) {
		return new BotReply(text, false);
	}

	public static BotReply withMenu(String text) {
		return new BotReply(text, true);
	}
}
