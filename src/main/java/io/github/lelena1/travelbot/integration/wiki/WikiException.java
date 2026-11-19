package io.github.lelena1.travelbot.integration.wiki;

/** Википедия или Викиданные не ответили. */
public class WikiException extends RuntimeException {

	public WikiException(String message, Throwable cause) {
		super(message, cause);
	}
}
