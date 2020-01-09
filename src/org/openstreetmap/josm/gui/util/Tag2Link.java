// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.tools.Utils;

/**
 * Extracts web links from OSM tags.
 *
 * @since xxx
 */
final class Tag2Link {

    // Related implementations:
    // - https://github.com/openstreetmap/openstreetmap-website/blob/master/app/helpers/browse_tags_helper.rb

    private Tag2Link() {
        // private constructor for utility class
    }

    @FunctionalInterface
    interface LinkConsumer {
        void acceptLink(String name, String url);
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
            linkConsumer.acceptLink(tr("View website"), value);
        }
        if (key.matches("^(.+[:_])?source([:_].+)?$") && valueIsURL) {
            linkConsumer.acceptLink(tr("View website"), value);
        }
        if (key.matches("^(.+[:_])?url([:_].+)?$") && valueIsURL) {
            linkConsumer.acceptLink(tr("View URL"), value);
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
                    .forEach(q -> linkConsumer.acceptLink(tr("View Wikidata item"), "https://www.wikidata.org/wiki/" + q));
        }
        if (key.matches("species")) {
            linkConsumer.acceptLink(tr("View Wikispecies page"), "https://species.wikimedia.org/wiki/" + value);
        }
        if (key.matches("wikimedia_commons|image") && value.matches("(?i:File):.*")) {
            linkConsumer.acceptLink(tr("View image on Wikimedia Commons"), "https://commons.wikimedia.org/wiki/" + value);
        }
        if (key.matches("wikimedia_commons|image") && value.matches("(?i:Category):.*")) {
            linkConsumer.acceptLink(tr("View category on Wikimedia Commons"), "https://commons.wikimedia.org/wiki/" + value);
        }

        // WHC
        if (key.matches("ref:whc") && (valueMatcher = Pattern.compile("(?<id>[0-9]+)(-.*)?").matcher(value)).matches()) {
            linkConsumer.acceptLink(tr("View UNESCO sheet"), "http://whc.unesco.org/en/list/" + valueMatcher.group("id"));
        }

        // Mapillary
        if (key.matches("((ref|source):)?mapillary") && value.matches("[0-9a-zA-Z-_]+")) {
            linkConsumer.acceptLink(tr("View {0} image", "Mapillary"), "https://www.mapillary.com/map/im/" + value);
        }

        // MMSI
        if (key.matches("seamark:(virtual_aton|radio_station):mmsi") && value.matches("[0-9]+")) {
            // https://en.wikipedia.org/wiki/Maritime_Mobile_Service_Identity
            linkConsumer.acceptLink(tr("View MMSI on MarineTraffic"),
                    "https://www.marinetraffic.com/en/ais/details/ships/shipid:/mmsi:" + value);
        }
    }

}
