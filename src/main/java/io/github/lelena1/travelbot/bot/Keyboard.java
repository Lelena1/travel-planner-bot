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

	/** Кнопка-ссылка: открывает сайт, боту ничего не отправляет. */
	record Link(String text, String url) {
	}

	/** Кнопки-ссылки под сообщением (например, «Купить на Aviasales»). */
	record Links(List<List<Link>> rows) implements Keyboard {

		public Links {
			rows = rows.stream().map(List::copyOf).toList();
		}
	}
}
