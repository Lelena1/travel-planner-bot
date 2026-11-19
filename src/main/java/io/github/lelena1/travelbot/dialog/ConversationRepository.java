package io.github.lelena1.travelbot.dialog;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

	List<Conversation> findByUpdatedAtBefore(Instant moment);
}
