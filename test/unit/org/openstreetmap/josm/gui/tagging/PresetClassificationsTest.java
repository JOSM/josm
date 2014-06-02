// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.Utils;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class PresetClassificationsTest {

    final static TaggingPresetSelector.PresetClassifications classifications = new TaggingPresetSelector.PresetClassifications();

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() throws IOException, SAXException {
        JOSMFixture.createUnitTestFixture().init();
        final Collection<TaggingPreset> presets = TaggingPresetReader.readAll("resource://data/defaultpresets.xml", true);
        classifications.loadPresets(presets);
    }

    private List<TaggingPresetSelector.PresetClassification> getMatchingPresets(String searchText, OsmPrimitive w) {
        return classifications.getMatchingPresets(searchText, true, true, EnumSet.of(TaggingPresetType.forPrimitive(w)), Collections.singleton(w));
    }

    private List<String> getMatchingPresetNames(String searchText, OsmPrimitive w) {
        return Utils.transform(getMatchingPresets(searchText, w), new Utils.Function<TaggingPresetSelector.PresetClassification, String>() {
            @Override
            public String apply(TaggingPresetSelector.PresetClassification x) {
                return x.preset.name;
            }
        });
    }

    @Test
    public void testBuilding() throws Exception {
        final Way w = new Way();
        final Node n1 = new Node();
        w.addNode(n1);
        w.addNode(new Node());
        w.addNode(new Node());
        assertTrue("unclosed way should not match building preset", !getMatchingPresetNames("building", w).contains("Building"));
        w.addNode(n1);
        assertTrue("closed way should match building preset", getMatchingPresetNames("building", w).contains("Building"));
    }

    @Test
    public void testRelationsForTram() {
        final OsmPrimitive tram = TestUtils.createPrimitive("way railway=tram");
        assertTrue("railway=tram should match 'Railway route' for relation creation", getMatchingPresetNames("route", tram).contains("Railway route"));
        assertTrue("railway=tram should match 'Public transport route' for relation creation", getMatchingPresetNames("route", tram).contains("Public transport route"));
        assertTrue("railway=tram should not match 'Bus route'", !getMatchingPresetNames("route", tram).contains("Bus route"));
    }

}
