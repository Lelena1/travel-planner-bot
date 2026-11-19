package io.github.lelena1.travelbot.trip;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Сохранённые поездки пользователя. */
@Service
public class TripService {

	private static final Logger log = LoggerFactory.getLogger(TripService.class);

	private final TripRepository trips;
	private final Clock clock;

	public TripService(TripRepository trips, Clock clock) {
		this.trips = trips;
		this.clock = clock;
	}

	/** Сохранённые поездки по дате начала; номер в списке = индекс + 1. */
	@Transactional(readOnly = true)
	public List<Trip> activeTrips(long userId) {
		return trips.findByUserIdAndStatusOrderByDateFromAsc(userId, TripStatus.ACTIVE);
	}

	/** Поездки, которые ещё не начались: только для них имеет смысл искать билеты. */
	@Transactional(readOnly = true)
	public List<Trip> upcomingTrips(long userId) {
		LocalDate today = LocalDate.now(clock);
		return activeTrips(userId).stream()
				.filter(trip -> !trip.getDateFrom().isBefore(today))
				.toList();
	}

	/**
	 * Удаляет поездку по её номеру в списке /mytrips.
	 *
	 * @return удалённая поездка или пусто, если такого номера нет
	 */
	@Transactional
	public Optional<Trip> deleteByNumber(long userId, int number) {
		List<Trip> list = activeTrips(userId);
		if (number < 1 || number > list.size()) {
			return Optional.empty();
		}
		Trip trip = list.get(number - 1);
		trips.delete(trip);
		log.info("Поездки: пользователь {} удалил поездку #{} ({})", userId, trip.getId(), trip.getDestination());
		return Optional.of(trip);
	}
}
