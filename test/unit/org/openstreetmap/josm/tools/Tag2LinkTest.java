// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.StaticClassCleanup;

/**
 * Test {@link Tag2Link}
 */
@BasicPreferences
@StaticClassCleanup(Tag2Link.class)
class Tag2LinkTest {

    List<String> links = new ArrayList<>();

    void addLink(String name, String url, ImageResource icon) {
        links.add(name + " // " + url);
    }

    void checkLinks(String... expected) {
        assertEquals(Arrays.asList(expected), this.links);
    }

    /**
     * Unit test of function {@link Tag2Link#initialize()}.
     */
    @Test
    void testInitialize() {
        Tag2Link.initialize();
        assertTrue(Tag2Link.wikidataRules.size() > 40, "obtains at least 40 rules");
    }

    /**
     * Unit test for links that may come in multiple forms.
     * Example: <a href="https://wiki.osm.org/wiki/Key:contact:facebook">https://wiki.openstreetmap.org/wiki/Key:contact:facebook</a>
     *
     * See also JOSM #21794
     * @param value The tag value for "contact:facebook"
     */
    @ParameterizedTest
    @ValueSource(strings = {"https://www.facebook.com/FacebookUserName", "FacebookUserName"})
    void testUrlKeyMultipleForms(final String value) {
        // We need the wikidata rules Since testInitialize tests initialization, reuse it.
        if (!Tag2Link.wikidataRules.containsKey("contact:facebook")) {
            this.testInitialize();
        }
        Tag2Link.getLinksForTag("contact:facebook", value, this::addLink);
        this.checkLinks("Open unavatar.now.sh // https://unavatar.now.sh/facebook/FacebookUserName",
                "Open facebook.com // https://www.facebook.com/FacebookUserName",
                "Open messenger.com // https://www.messenger.com/t/FacebookUserName");
    }

    /**
     * Unit test of function {@link Tag2Link#getLinksForTag}.
     */
    @Test
    void testName() {
        Tag2Link.getLinksForTag("name", "foobar", this::addLink);
        checkLinks("Search on duckduckgo.com // https://duckduckgo.com/?q=foobar",
                "Search on google.com // https://www.google.com/search?q=foobar");
    }

    /**
     * Unit test of function {@link Tag2Link#getLinksForTag}.
     */
    @Test
    void testWebsite() {
        Tag2Link.getLinksForTag("website", "http://www.openstreetmap.org/", this::addLink);
        checkLinks("Open openstreetmap.org // http://www.openstreetmap.org/");
        links.clear();
        Tag2Link.getLinksForTag("website", "https://www.openstreetmap.org/", this::addLink);
        checkLinks("Open openstreetmap.org // https://www.openstreetmap.org/");
        links.clear();
        Tag2Link.getLinksForTag("website", "www.openstreetmap.org", this::addLink);
        checkLinks("Open openstreetmap.org // http://www.openstreetmap.org");
    }

    /**
     * Unit test of function {@link Tag2Link#getLinksForTag}.
     */
    @Test
    void testWikipedia() {
        Tag2Link.getLinksForTag("wikipedia", "de:Wohnhausgruppe Herderstraße", this::addLink);
        checkLinks("View Wikipedia article // https://de.wikipedia.org/wiki/Wohnhausgruppe_Herderstraße");
        links.clear();
        Tag2Link.getLinksForTag("wikipedia", "de:Stadtbahn Köln#Innenstadttunnel", this::addLink);
        checkLinks("View Wikipedia article // https://de.wikipedia.org/wiki/Stadtbahn_Köln#Innenstadttunnel");
    }

    /**
     * Unit test of function {@link Tag2Link#getLinksForTag}.
     */
    @Test
    void testImageCommonsImage() {
        Tag2Link.getLinksForTag("image", "File:Witten Brücke Gasstraße.jpg", this::addLink);
        checkLinks("View image on Wikimedia Commons // https://commons.wikimedia.org/wiki/File%3AWitten_Br%C3%BCcke_Gasstra%C3%9Fe.jpg");
        links.clear();
        // non-regression test for #19754
        Tag2Link.getLinksForTag("image", "File:Foo.jpg;File:Bar.jpg", this::addLink);
        checkLinks("View image on Wikimedia Commons // https://commons.wikimedia.org/wiki/File%3AFoo.jpg",
                "View image on Wikimedia Commons // https://commons.wikimedia.org/wiki/File%3ABar.jpg");
        links.clear();
        // non-regression test for #19771
        Tag2Link.getLinksForTag("image", "File:Côte de granite rose - Trégastel à Ploumanac'h - 20190723 - 025.jpg", this::addLink);
        checkLinks("View image on Wikimedia Commons // " +
                "https://commons.wikimedia.org/wiki/" +
                "File%3AC%C3%B4te_de_granite_rose_-_Tr%C3%A9gastel_%C3%A0_Ploumanac%27h_-_20190723_-_025.jpg");
    }

    /**
     * Unit test of function {@link Tag2Link#getLinksForTag}.
     */
    @Test
    void testImageCommonsCategory() {
        Tag2Link.getLinksForTag("image", "category:JOSM", this::addLink);
        checkLinks("View category on Wikimedia Commons // https://commons.wikimedia.org/wiki/category%3AJOSM");
    }

    /**
     * Unit test of function {@link Tag2Link#getLinksForTag}.
     */
    @Test
    void testBrandWikidata() {
        Tag2Link.getLinksForTag("brand:wikidata", "Q259340", this::addLink);
        checkLinks("View Wikidata item // https://www.wikidata.org/wiki/Q259340");
    }

    /**
     * Unit test of function {@link Tag2Link#getLinksForTag}.
     */
    @Test
    void testArchipelagoWikidata() {
        Tag2Link.getLinksForTag("archipelago:wikidata", "Q756987;Q756988", this::addLink);
        checkLinks("View Wikidata item // https://www.wikidata.org/wiki/Q756987",
                "View Wikidata item // https://www.wikidata.org/wiki/Q756988");
    }

    /**
     * Unit test of function {@link Tag2Link#getLinksForTag}.
     *
     * Non-regression test for https://josm.openstreetmap.de/ticket/19754#comment:9
     */
    @Test
    void testMultipleSources() {
        Tag2Link.getLinksForTag("source", "https://foo.com/; https://bar.com/; https://baz.com/", this::addLink);
        checkLinks("Open foo.com // https://foo.com/",
                "Open bar.com // https://bar.com/",
                "Open baz.com // https://baz.com/");
    }

}
