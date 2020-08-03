// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonReader;
import javax.json.JsonValue;

import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.preferences.CachingProperty;
import org.openstreetmap.josm.data.preferences.ListProperty;
import org.openstreetmap.josm.io.CachedFile;

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

    static final ListProperty PREF_SOURCE = new ListProperty("tag2link.source",
            Collections.singletonList("resource://META-INF/resources/webjars/tag2link/2020.8.3/index.json"));

    static final CachingProperty<List<String>> PREF_SEARCH_ENGINES = new ListProperty("tag2link.search",
            Arrays.asList("https://duckduckgo.com/?q=$1", "https://www.google.com/search?q=$1")).cached();

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
         * @param icon the icon to use
         */
        void acceptLink(String name, String url, ImageResource icon);
    }

    /**
     * Initializes the tag2link rules
     */
    public static void initialize() {
        try {
            wikidataRules.clear();
            for (String source : PREF_SOURCE.get()) {
                initializeFromResources(new CachedFile(source));
            }
        } catch (Exception e) {
            Logging.error("Failed to initialize tag2link rules");
            Logging.error(e);
        }
    }

    /**
     * Initializes the tag2link rules from the resources.
     *
     * @param resource the source
     * @throws IOException in case of I/O error
     */
    private static void initializeFromResources(CachedFile resource) throws IOException {
        final JsonArray rules;
        try (InputStream inputStream = resource.getInputStream();
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
        wikidataRules.keySet().removeIf(key -> key.matches("^(.+[:_])?website([:_].+)?$")
                || key.matches("^(.+[:_])?url([:_].+)?$")
                || key.matches("wikimedia_commons|image")
                || key.matches("wikipedia(:(?<lang>\\p{Lower}{2,}))?")
                || key.matches("(.*:)?wikidata"));

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

        final HashMap<OsmPrimitiveType, Optional<ImageResource>> memoize = new HashMap<>();
        final Supplier<ImageResource> imageResource = () -> memoize
                .computeIfAbsent(OsmPrimitiveType.NODE, type -> OsmPrimitiveImageProvider.getResource(key, value, type))
                .orElse(null);

        // Search
        if (key.matches("^(.+[:_])?name([:_]" + languagePattern + ")?$")) {
            final ImageResource search = new ImageProvider("dialogs/search").getResource();
            PREF_SEARCH_ENGINES.get().forEach(url ->
                    linkConsumer.acceptLink(tr("Search on {0}", getHost(url, url)), url.replace("$1", Utils.encodeUrl(value)), search));
        }

        // Common
        final String validURL = value.startsWith("http:") || value.startsWith("https:")
                ? value
                : value.startsWith("www.")
                ? "http://" + value
                : null;
        if (key.matches("^(.+[:_])?website([:_].+)?$") && validURL != null) {
            linkConsumer.acceptLink(getLinkName(validURL, key), validURL, imageResource.get());
        }
        if (key.matches("^(.+[:_])?source([:_].+)?$") && validURL != null) {
            linkConsumer.acceptLink(getLinkName(validURL, key), validURL, imageResource.get());
        }
        if (key.matches("^(.+[:_])?url([:_].+)?$") && validURL != null) {
            linkConsumer.acceptLink(getLinkName(validURL, key), validURL, imageResource.get());
        }
        if (key.matches("image") && validURL != null) {
            linkConsumer.acceptLink(tr("View image"), validURL, imageResource.get());
        }

        // Wikimedia
        final Matcher keyMatcher = Pattern.compile("wikipedia(:(?<lang>\\p{Lower}{2,}))?").matcher(key);
        final Matcher valueMatcher = Pattern.compile("((?<lang>\\p{Lower}{2,}):)?(?<article>.*)").matcher(value);
        if (keyMatcher.matches() && valueMatcher.matches()) {
            final String lang = Utils.firstNotEmptyString("en", keyMatcher.group("lang"), valueMatcher.group("lang"));
            final String url = "https://" + lang + ".wikipedia.org/wiki/" + valueMatcher.group("article").replace(' ', '_');
            linkConsumer.acceptLink(tr("View Wikipedia article"), url, imageResource.get());
        }
        if (key.matches("(.*:)?wikidata")) {
            OsmUtils.splitMultipleValues(value)
                    .forEach(q -> linkConsumer.acceptLink(tr("View Wikidata item"), "https://www.wikidata.org/wiki/" + q, imageResource.get()));
        }
        if (key.matches("(.*:)?species")) {
            final String url = "https://species.wikimedia.org/wiki/" + value;
            linkConsumer.acceptLink(getLinkName(url, key), url, imageResource.get());
        }
        if (key.matches("wikimedia_commons|image") && value.matches("(?i:File):.*")) {
            String url = "https://commons.wikimedia.org/wiki/" + value;
            linkConsumer.acceptLink(tr("View image on Wikimedia Commons"), url, imageResource.get());
        }
        if (key.matches("wikimedia_commons|image") && value.matches("(?i:Category):.*")) {
            String url = "https://commons.wikimedia.org/wiki/" + value;
            linkConsumer.acceptLink(tr("View category on Wikimedia Commons"), url, imageResource.get());
        }

        wikidataRules.getValues(key).forEach(urlFormatter -> {
            final String formattedValue = valueFormatter.getOrDefault(key, x -> x).apply(value);
            final String url = urlFormatter.replace("$1", formattedValue);
            linkConsumer.acceptLink(getLinkName(url, key), url, imageResource.get());
        });
    }

    private static String getLinkName(String url, String fallback) {
        return tr("Open {0}", getHost(url, fallback));
    }

    private static String getHost(String url, String fallback) {
        try {
            return new URL(url).getHost().replaceFirst("^www\\.", "");
        } catch (MalformedURLException e) {
            return fallback;
        }
    }

}
