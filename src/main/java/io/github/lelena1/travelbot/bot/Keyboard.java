package io.github.lelena1.travelbot.bot;

import java.util.List;

/** Какие кнопки показать под сообщением бота. */
public sealed interface Keyboard {

	/** Не трогать клавиатуру, которая уже показана. */
	Keyboard KEEP = new Keep();

	/** Убрать клавиатуру: пользователь вводит ответ сам (город, даты). */
	Keyboard REMOVE = new Remove();

	/** Главное меню. */
	Keyboard MAIN_MENU = new MainMenuKeyboard();

	static Keyboard options(List<List<String>> rows) {
		return new Options(rows);
	}

	static Keyboard links(List<List<Link>> rows) {
		return new Links(rows);
	}

	record Keep() implements Keyboard {
	}

	record Remove() implements Keyboard {
	}

	record MainMenuKeyboard() implements Keyboard {
	}

	/** Варианты ответа, по рядам. */
	record Options(List<List<String>> rows) implements Keyboard {

		public Options {
			rows = rows.stream().map(List::copyOf).toList();
		}
	}

	/**
	 * Кнопка под сообщением: либо ссылка (открывает сайт), либо действие —
	 * при нажатии бот получает {@code callbackData}, например «watch:12».
	 */
	record Link(String text, String url, String callbackData) {

		/** Telegram ограничивает данные кнопки 64 байтами. */
		static final int MAX_CALLBACK_BYTES = 64;

		public Link {
			if ((url == null) == (callbackData == null)) {
				throw new IllegalArgumentException("Нужна либо ссылка, либо действие");
			}
			if (callbackData != null
					&& callbackData.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_CALLBACK_BYTES) {
				throw new IllegalArgumentException("Данные кнопки длиннее 64 байт: " + callbackData);
			}
		}

		/** Кнопка-ссылка. */
		public Link(String text, String url) {
			this(text, url, null);
		}

		public static Link action(String text, String callbackData) {
			return new Link(text, null, callbackData);
		}

		public boolean isAction() {
			return callbackData != null;
		}
	}

	/** Кнопки-ссылки под сообщением (например, «Купить на Aviasales»). */
	record Links(List<List<Link>> rows) implements Keyboard {

		public Links {
			rows = rows.stream().map(List::copyOf).toList();
		}
	}
}
