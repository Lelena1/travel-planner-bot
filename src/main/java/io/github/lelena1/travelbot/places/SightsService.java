package io.github.lelena1.travelbot.places;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import io.github.lelena1.travelbot.city.GeoPoint;
import io.github.lelena1.travelbot.config.CacheConfig;
import io.github.lelena1.travelbot.integration.wiki.WikiArticle;
import io.github.lelena1.travelbot.integration.wiki.WikidataClient;
import io.github.lelena1.travelbot.integration.wiki.WikidataFacts;
import io.github.lelena1.travelbot.integration.wiki.WikipediaClient;
import io.github.lelena1.travelbot.util.CityNames;

/**
 * Главные достопримечательности города.
 * <p>
 * Берём статьи Википедии в радиусе 10 км от центра, отсеиваем то, что не является местом
 * (сам город, районы, улицы, события, заводы), и сортируем по известности —
 * в скольких языковых Википедиях есть статья. Так Колизей оказывается выше местного сквера.
 */
@Service
public class SightsService {

	private static final Logger log = LoggerFactory.getLogger(SightsService.class);

	static final int RADIUS_METERS = 10_000;
	static final int ARTICLES_TO_CHECK = 200;
	static final int TOP = 7;
	/** Статья хотя бы на двух языках — иначе это скорее местная мелочь. */
	static final int MIN_FAME = 2;

	/** Типы координат Википедии, которые не являются достопримечательностями. */
	static final Set<String> NOT_SIGHT_TYPES = Set.of("city", "country", "state", "adm1st", "adm2nd", "adm3rd",
			"airport", "event", "railwaystation");

	/** Слова в названии или описании, которые выдают «не место для туриста». */
	static final List<String> NOT_SIGHT_WORDS = List.of("улица", "район", "станция", "завод", "микрорайон",
			"проспект", "переулок", "шоссе", "округ", "деревня", "село", "посёлок", "поселок", "квартал", "компания",
			"школа", "больница", "университет", "институт", "колледж", "линия");

	/** Исторические области и государства (Викиданные, P31). */
	static final Set<String> NOT_SIGHT_KINDS = Set.of("Q1620908", "Q3024240", "Q19953632");

	private final WikipediaClient wikipedia;
	private final WikidataClient wikidata;

	public SightsService(WikipediaClient wikipedia, WikidataClient wikidata) {
		this.wikipedia = wikipedia;
		this.wikidata = wikidata;
	}

	/** Кешируется на неделю: достопримечательности не меняются, а запросов к Википедии несколько. */
	@Cacheable(cacheNames = CacheConfig.SIGHTS, key = "#cityName")
	public List<Sight> topSights(String cityName, GeoPoint center) {
		List<WikiArticle> articles = wikipedia.nearby(center, RADIUS_METERS, ARTICLES_TO_CHECK).stream()
				.filter(article -> looksLikeSight(article, cityName))
				.toList();
		Map<String, WikidataFacts> facts = wikidata.facts(articles.stream().map(WikiArticle::wikidataId).toList());

		List<WikiArticle> best = articles.stream()
				.filter(article -> article.wikidataId() != null && facts.containsKey(article.wikidataId()))
				.filter(article -> isPlace(facts.get(article.wikidataId())))
				.filter(article -> fame(article, facts) >= MIN_FAME)
				.sorted(Comparator.comparingInt((WikiArticle article) -> fame(article, facts)).reversed())
				.limit(TOP)
				.toList();

		Map<String, String> intros = wikipedia.introductions(best.stream().map(WikiArticle::title).toList());
		log.info("Места: {} — из {} статей рядом выбрано {}", cityName, articles.size(), best.size());
		return best.stream()
				.map(article -> new Sight(article.title(), article.description(), intros.get(article.title()),
						article.point(), WikipediaClient.articleUrl(article.title()), fame(article, facts)))
				.toList();
	}

	static boolean looksLikeSight(WikiArticle article, String cityName) {
		if (article.coordinateType() != null && NOT_SIGHT_TYPES.contains(article.coordinateType())) {
			return false;
		}
		if (CityNames.normalize(article.title()).equals(CityNames.normalize(cityName))) {
			return false;
		}
		String text = (article.title() + " " + (article.description() == null ? "" : article.description()))
				.toLowerCase(Locale.ROOT);
		return NOT_SIGHT_WORDS.stream().noneMatch(text::contains);
	}

	static boolean isPlace(WikidataFacts facts) {
		return !facts.hasPopulation() && !facts.isEvent()
				&& facts.instanceOf().stream().noneMatch(NOT_SIGHT_KINDS::contains);
	}

	private static int fame(WikiArticle article, Map<String, WikidataFacts> facts) {
		return facts.get(article.wikidataId()).sitelinks();
	}
}
