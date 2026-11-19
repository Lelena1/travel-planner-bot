package io.github.lelena1.travelbot.pricewatch;

/**
 * Стоит ли сообщать пользователю об изменении цены.
 * Мелкие колебания (меньше 3% и меньше 300 ₽) не повод писать — иначе уведомлений будет слишком много.
 */
public enum PriceChange {

	/** Раньше цен на даты поездки не было, а теперь появились. */
	APPEARED,
	/** Цена заметно снизилась. */
	DROPPED,
	/** Ничего важного. */
	NONE;

	static final double MIN_DROP_SHARE = 0.03;
	static final int MIN_DROP_RUBLES = 300;

	public static PriceChange of(Integer previous, Integer current) {
		if (current == null) {
			return NONE;
		}
		if (previous == null) {
			return APPEARED;
		}
		int drop = previous - current;
		boolean noticeable = drop >= MIN_DROP_RUBLES && drop >= previous * MIN_DROP_SHARE;
		return noticeable ? DROPPED : NONE;
	}

	/** Насколько подешевело, в процентах (округление вниз, как на ценниках «−6%»). */
	public static int dropPercent(int previous, int current) {
		return (int) ((previous - current) * 100L / previous);
	}
}
