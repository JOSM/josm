// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test {@link Tag2Link}
 */
public class Tag2LinkTest {

    List<String> links = new ArrayList<>();

    void addLink(String name, String url) {
        links.add(name + " // " + url);
    }

    void checkLinks(String... expected) {
        Assert.assertEquals(Arrays.asList(expected), links);
    }

    /**
     * Unit test of function {@link Tag2Link#getLinksForTag}.
     */
    @Test
    public void testImageCommonsImage() {
        Tag2Link.getLinksForTag("image", "File:Witten Brücke Gasstraße.jpg", this::addLink);
        checkLinks("View image on Wikimedia Commons // https://commons.wikimedia.org/wiki/File:Witten Brücke Gasstraße.jpg");
    }

    /**
     * Unit test of function {@link Tag2Link#getLinksForTag}.
     */
    @Test
    public void testImageCommonsCategory() {
        Tag2Link.getLinksForTag("image", "category:JOSM", this::addLink);
        checkLinks("View category on Wikimedia Commons // https://commons.wikimedia.org/wiki/category:JOSM");
    }

    /**
     * Unit test of function {@link Tag2Link#getLinksForTag}.
     */
    @Test
    public void testBrandWikidata() {
        Tag2Link.getLinksForTag("brand:wikidata", "Q259340", this::addLink);
        checkLinks("View Wikidata item Q259340 // https://www.wikidata.org/wiki/Q259340");
    }

    /**
     * Unit test of function {@link Tag2Link#getLinksForTag}.
     */
    @Test
    public void testArchipelagoWikidata() {
        Tag2Link.getLinksForTag("archipelago:wikidata", "Q756987;Q756988", this::addLink);
        checkLinks("View Wikidata item Q756987 // https://www.wikidata.org/wiki/Q756987",
                "View Wikidata item Q756988 // https://www.wikidata.org/wiki/Q756988");
    }

}
