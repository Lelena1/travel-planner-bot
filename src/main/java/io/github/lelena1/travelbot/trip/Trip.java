package io.github.lelena1.travelbot.trip;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Поездка: куда, откуда, когда и в каком составе. */
@Entity
@Table(name = "trip")
public class Trip {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private TripStatus status;

	private String destination;

	private String origin;

	@Column(name = "date_from")
	private LocalDate dateFrom;

	@Column(name = "date_to")
	private LocalDate dateTo;

	private Integer adults;

	private Integer children;

	@Convert(converter = IntegerListConverter.class)
	@Column(name = "child_ages")
	private List<Integer> childAges = List.of();

	@Enumerated(EnumType.STRING)
	private Pets pets;

	@Enumerated(EnumType.STRING)
	private Budget budget;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected Trip() {
		// для JPA
	}

	public static Trip draftFor(long userId) {
		Trip trip = new Trip();
		trip.userId = userId;
		trip.status = TripStatus.DRAFT;
		trip.createdAt = Instant.now();
		return trip;
	}

	/** Количество ночей: с 12 по 18 мая — 6 ночей. */
	public long nights() {
		return ChronoUnit.DAYS.between(dateFrom, dateTo);
	}

	public int travelersCount() {
		return (adults == null ? 0 : adults) + (children == null ? 0 : children);
	}

	public void activate() {
		this.status = TripStatus.ACTIVE;
	}

	public Long getId() {
		return id;
	}

	public Long getUserId() {
		return userId;
	}

	public TripStatus getStatus() {
		return status;
	}

	public String getDestination() {
		return destination;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(String origin) {
		this.origin = origin;
	}

	public LocalDate getDateFrom() {
		return dateFrom;
	}

	public LocalDate getDateTo() {
		return dateTo;
	}

	public void setDates(LocalDate from, LocalDate to) {
		this.dateFrom = from;
		this.dateTo = to;
	}

	public Integer getAdults() {
		return adults;
	}

	public void setAdults(Integer adults) {
		this.adults = adults;
	}

	public Integer getChildren() {
		return children;
	}

	public void setChildren(Integer children) {
		this.children = children;
	}

	public List<Integer> getChildAges() {
		return childAges;
	}

	public void setChildAges(List<Integer> childAges) {
		this.childAges = List.copyOf(childAges);
	}

	public Pets getPets() {
		return pets;
	}

	public void setPets(Pets pets) {
		this.pets = pets;
	}

	public Budget getBudget() {
		return budget;
	}

	public void setBudget(Budget budget) {
		this.budget = budget;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
