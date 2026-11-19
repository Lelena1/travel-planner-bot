package io.github.lelena1.travelbot.pricewatch;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceWatchRepository extends JpaRepository<PriceWatch, Long> {

	List<PriceWatch> findByUserIdOrderByCreatedAtAsc(long userId);

	Optional<PriceWatch> findByTripId(long tripId);

	long countByUserId(long userId);
}
