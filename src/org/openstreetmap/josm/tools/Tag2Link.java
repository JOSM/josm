// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonReader;
import javax.json.JsonValue;

import org.openstreetmap.josm.data.osm.OsmUtils;

/**
 * Extracts web links from OSM tags.
 * 
 * The following rules are used:
 * <ul>
 * <li>internal rules for basic tags</li>
 * <li>rules from Wikidata based on OSM tag or key (P1282); formatter URL (P1630); third-party formatter URL (P3303)</li>
 * <li>rules from OSM Sophox based on permanent key ID (P16); formatter URL (P8)</li>
 * </ul>
 *
 * @since 15673
 */
public final class Tag2Link {

    // Related implementations:
    // - https://github.com/openstreetmap/openstreetmap-website/blob/master/app/helpers/browse_tags_helper.rb

    /**
     * Maps OSM keys to formatter URLs from Wikidata and OSM Sophox where {@code "$1"} has to be replaced by a value.
     */
    static final MultiMap<String, String> wikidataRules = new MultiMap<>();

    static final Map<String, UnaryOperator<String>> valueFormatter = Collections.singletonMap(
            "ref:bag", v -> String.format("%16s", v).replace(' ', '0')
    );

    static final String languagePattern = LanguageInfo.getLanguageCodes(null).stream()
            .map(Pattern::quote)
            .collect(Collectors.joining("|"));

    private Tag2Link() {
        // private constructor for utility class
    }

    /**
     * Represents an operation that accepts a link.
     */
    @FunctionalInterface
    public interface LinkConsumer {
        /**
         * Performs the operation on the given arguments.
         * @param name the name/label of the link
         * @param url the URL of the link
         */
        void acceptLink(String name, String url);
    }

    /**
     * Initializes the tag2link rules
     */
    public static void initialize() {
        try {
            wikidataRules.clear();
            initializeFromResources();
        } catch (Exception e) {
            Logging.error("Failed to initialize tag2link rules");
            Logging.error(e);
        }
    }

    /**
     * Initializes the tag2link rules from the resources.
     *
     * @throws IOException in case of I/O error
     */
    private static void initializeFromResources() throws IOException {
        final String resource = "META-INF/resources/webjars/tag2link/2020.7.15/index.json";
        final JsonArray rules;
        try (InputStream inputStream = Tag2Link.class.getClassLoader().getResourceAsStream(resource);
             JsonReader jsonReader = Json.createReader(inputStream)) {
            rules = jsonReader.readArray();
        }

        for (JsonValue rule : rules) {
            final String key = rule.asJsonObject().getString("key");
            final String url = rule.asJsonObject().getString("url");
            if (key.startsWith("Key:")) {
                wikidataRules.put(key.substring("Key:".length()), url);
            }
        }
        // We handle those keys ourselves
        Stream.of("image", "url", "website", "wikidata", "wikimedia_commons")
                .forEach(wikidataRules::remove);

        final int size = wikidataRules.size();
        Logging.info(trn(
                "Obtained {0} Tag2Link rule from {1}",
                "Obtained {0} Tag2Link rules from {1}",
                size, size, resource));
    }

    /**
     * Generates the links for the tag given by {@code key} and {@code value}, and sends 0, 1 or more links to the {@code linkConsumer}.
     * @param key the tag key
     * @param value the tag value
     * @param linkConsumer the receiver of the generated links
     */
    public static void getLinksForTag(String key, String value, LinkConsumer linkConsumer) {

        if (value == null || value.isEmpty()) {
            return;
        }

        // Search
        if (key.matches("^(.+[:_])?name([:_]" + languagePattern + ")?$")) {
            linkConsumer.acceptLink(tr("Search on DuckDuckGo"), "https://duckduckgo.com/?q=" + value);
        }

        // Common
        final String validURL = value.startsWith("http:") || value.startsWith("https:")
                ? value
                : value.startsWith("www.")
                ? "http://" + value
                : null;
        if (key.matches("^(.+[:_])?website([:_].+)?$") && validURL != null) {
            linkConsumer.acceptLink(getLinkName(validURL, key), validURL);
        }
        if (key.matches("^(.+[:_])?source([:_].+)?$") && validURL != null) {
            linkConsumer.acceptLink(getLinkName(validURL, key), validURL);
        }
        if (key.matches("^(.+[:_])?url([:_].+)?$") && validURL != null) {
            linkConsumer.acceptLink(getLinkName(validURL, key), validURL);
        }
        if (key.matches("image") && validURL != null) {
            linkConsumer.acceptLink(tr("View image"), validURL);
        }

        // Wikimedia
        final Matcher keyMatcher = Pattern.compile("wikipedia(:(?<lang>\\p{Lower}{2,}))?").matcher(key);
        final Matcher valueMatcher = Pattern.compile("((?<lang>\\p{Lower}{2,}):)?(?<article>.*)").matcher(value);
        if (keyMatcher.matches() && valueMatcher.matches()) {
            final String lang = Utils.firstNotEmptyString("en", keyMatcher.group("lang"), valueMatcher.group("lang"));
            final String url = "https://" + lang + ".wikipedia.org/wiki/" + valueMatcher.group("article").replace(' ', '_');
            linkConsumer.acceptLink(tr("View Wikipedia article"), url);
        }
        if (key.matches("(.*:)?wikidata")) {
            OsmUtils.splitMultipleValues(value)
                    .forEach(q -> linkConsumer.acceptLink(tr("View Wikidata item"), "https://www.wikidata.org/wiki/" + q));
        }
        if (key.matches("(.*:)?species")) {
            final String url = "https://species.wikimedia.org/wiki/" + value;
            linkConsumer.acceptLink(getLinkName(url, key), url);
        }
        if (key.matches("wikimedia_commons|image") && value.matches("(?i:File):.*")) {
            linkConsumer.acceptLink(tr("View image on Wikimedia Commons"), "https://commons.wikimedia.org/wiki/" + value);
        }
        if (key.matches("wikimedia_commons|image") && value.matches("(?i:Category):.*")) {
            linkConsumer.acceptLink(tr("View category on Wikimedia Commons"), "https://commons.wikimedia.org/wiki/" + value);
        }

        wikidataRules.getValues(key).forEach(urlFormatter -> {
            final String formattedValue = valueFormatter.getOrDefault(key, x -> x).apply(value);
            final String url = urlFormatter.replace("$1", formattedValue);
            linkConsumer.acceptLink(getLinkName(url, key), url);
        });
    }

    private static String getLinkName(String url, String fallback) {
        try {
            return tr("Open {0}", new URL(url).getHost());
        } catch (MalformedURLException e) {
            return tr("Open {0}", fallback);
        }
    }

}
