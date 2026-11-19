package io.github.lelena1.travelbot.bot;

import java.util.List;

/**
 * Ответ бота.
 *
 * @param text     текст сообщения
 * @param keyboard кнопки под сообщением
 */
public record BotReply(String text, Keyboard keyboard) {

	public static BotReply text(String text) {
		return new BotReply(text, Keyboard.KEEP);
	}

	public static BotReply withMenu(String text) {
		return new BotReply(text, Keyboard.MAIN_MENU);
	}

	public static BotReply withOptions(String text, List<List<String>> rows) {
		return new BotReply(text, Keyboard.options(rows));
	}

	/** Убрать кнопки: ответ пользователь пишет сам. */
	public static BotReply freeInput(String text) {
		return new BotReply(text, Keyboard.REMOVE);
	}

	public boolean showsMainMenu() {
		return keyboard instanceof Keyboard.MainMenuKeyboard;
	}
}
