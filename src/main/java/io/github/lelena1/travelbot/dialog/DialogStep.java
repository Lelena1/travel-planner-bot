package io.github.lelena1.travelbot.dialog;

/** Шаги анкеты «Новая поездка» — состояния конечного автомата. */
public enum DialogStep {
	DESTINATION,
	ORIGIN,
	DATES,
	ADULTS,
	CHILDREN,
	CHILD_AGES,
	PETS,
	BUDGET,
	CONFIRM
}
