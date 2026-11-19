package io.github.lelena1.travelbot.trip;

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

	public TripService(TripRepository trips) {
		this.trips = trips;
	}

	/** Сохранённые поездки по дате начала; номер в списке = индекс + 1. */
	@Transactional(readOnly = true)
	public List<Trip> activeTrips(long userId) {
		return trips.findByUserIdAndStatusOrderByDateFromAsc(userId, TripStatus.ACTIVE);
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
