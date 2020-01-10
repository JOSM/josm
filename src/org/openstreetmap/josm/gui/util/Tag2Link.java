// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonValue;

import com.drew.lang.Charsets;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.MultiMap;
import org.openstreetmap.josm.tools.Utils;

/**
 * Extracts web links from OSM tags.
 *
 * @since 15673
 */
public final class Tag2Link {

    // Related implementations:
    // - https://github.com/openstreetmap/openstreetmap-website/blob/master/app/helpers/browse_tags_helper.rb

    /**
     * Maps OSM keys to formatter URLs from Wikidata where {@code "$1"} has to be replaced by a value.
     */
    protected static MultiMap<String, String> wikidataRules = new MultiMap<>();

    private Tag2Link() {
        // private constructor for utility class
    }

    @FunctionalInterface
    interface LinkConsumer {
        void acceptLink(String name, String url);
    }

    /**
     * Initializes the tag2link rules
     */
    public static void initialize() {
        try {
            fetchRulesFromWikidata();
        } catch (Exception e) {
            Logging.error("Failed to initialize tag2link rules");
            Logging.error(e);
        }
    }

    /**
     * Fetches rules from Wikidata using a SPARQL query.
     *
     * @throws IOException in case of I/O error
     */
    private static void fetchRulesFromWikidata() throws IOException {
        final String sparql = new String(new CachedFile("resource://data/tag2link.sparql").getByteContent(), Charsets.UTF_8);
        final CachedFile sparqlFile = new CachedFile("https://query.wikidata.org/sparql?query=" + Utils.encodeUrl(sparql))
                .setHttpAccept("application/json");

        final JsonArray rules;
        try (BufferedReader reader = sparqlFile.getContentReader()) {
            rules = Json.createReader(reader).read().asJsonObject().getJsonObject("results").getJsonArray("bindings");
        }

        for (JsonValue rule : rules) {
            final String key = rule.asJsonObject().getJsonObject("OSM_key").getString("value");
            final String url = rule.asJsonObject().getJsonObject("formatter_URL").getString("value");
            if (key.startsWith("Key:")) {
                wikidataRules.put(key.substring("Key:".length()), url);
            }
        }
        // We handle those keys ourselves
        Stream.of("image", "url", "website", "wikidata", "wikimedia_commons")
                .forEach(wikidataRules::remove);

        Logging.info(trn(
                "Obtained {0} Tag2Link rule from {1}",
                "Obtained {0} Tag2Link rules from {1}",
                wikidataRules.size(), wikidataRules.size(), "Wikidata"));
    }

    static void getLinksForTag(String key, String value, LinkConsumer linkConsumer) {
        Matcher keyMatcher;
        Matcher valueMatcher;

        // Search
        if (key.matches("^(.+[:_])?name([:_].+)?$")) {
            linkConsumer.acceptLink(tr("Search on DuckDuckGo"), "https://duckduckgo.com/?q=" + value);
        }

        // Common
        final boolean valueIsURL = value.matches("^(http:|https:|www\\.).*");
        if (key.matches("^(.+[:_])?website([:_].+)?$") && valueIsURL) {
            linkConsumer.acceptLink(getLinkName(value, key), value);
        }
        if (key.matches("^(.+[:_])?source([:_].+)?$") && valueIsURL) {
            linkConsumer.acceptLink(getLinkName(value, key), value);
        }
        if (key.matches("^(.+[:_])?url([:_].+)?$") && valueIsURL) {
            linkConsumer.acceptLink(getLinkName(value, key), value);
        }
        if (key.matches("image") && valueIsURL) {
            linkConsumer.acceptLink(tr("View image"), value);
        }

        // Wikimedia
        if ((keyMatcher = Pattern.compile("wikipedia(:(?<lang>\\p{Lower}{2,}))?").matcher(key)).matches()
                && (valueMatcher = Pattern.compile("((?<lang>\\p{Lower}{2,}):)?(?<article>.*)").matcher(value)).matches()) {
            final String lang = Utils.firstNotEmptyString("en", keyMatcher.group("lang"), valueMatcher.group("lang"));
            linkConsumer.acceptLink(tr("View Wikipedia article"), "https://" + lang + ".wikipedia.org/wiki/" + valueMatcher.group("article"));
        }
        if (key.matches("(.*:)?wikidata")) {
            OsmUtils.splitMultipleValues(value)
                    .forEach(q -> linkConsumer.acceptLink(tr("View Wikidata item {0}", q), "https://www.wikidata.org/wiki/" + q));
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
            final String url = urlFormatter.replace("$1", value);
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
