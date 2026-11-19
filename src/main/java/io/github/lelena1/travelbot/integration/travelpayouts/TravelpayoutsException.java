package io.github.lelena1.travelbot.integration.travelpayouts;

/** Сервис Travelpayouts не ответил или ответил ошибкой. */
public class TravelpayoutsException extends RuntimeException {

	public TravelpayoutsException(String message, Throwable cause) {
		super(message, cause);
	}

	public TravelpayoutsException(String message) {
		super(message);
	}
}
