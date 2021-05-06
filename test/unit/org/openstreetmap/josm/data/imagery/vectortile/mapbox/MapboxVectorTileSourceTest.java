// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery.vectortile.mapbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


import java.util.stream.Stream;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.vectortile.mapbox.style.MapboxVectorStyle;
import org.openstreetmap.josm.data.imagery.vectortile.mapbox.style.Source;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.widgets.JosmComboBox;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.testutils.mockers.ExtendedDialogMocker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test class for {@link MapboxVectorTileSource}
 * @author Taylor Smock
 * @since 17862
 */
class MapboxVectorTileSourceTest {
    @RegisterExtension
    JOSMTestRules rule = new JOSMTestRules();
    private static class SelectLayerDialogMocker extends ExtendedDialogMocker {
        int index;
        @Override
        protected void act(final ExtendedDialog instance) {
            ((JosmComboBox<?>) this.getContent(instance)).setSelectedIndex(index);
        }

        @Override
        protected String getString(final ExtendedDialog instance) {
            return String.join(";", ((Source) ((JosmComboBox<?>) this.getContent(instance)).getSelectedItem()).getUrls());
        }
    }

    @Test
    void testNoStyle() {
        MapboxVectorTileSource tileSource = new MapboxVectorTileSource(
          new ImageryInfo("Test Mapillary", "file:/" + TestUtils.getTestDataRoot() + "pbf/mapillary/{z}/{x}/{y}.mvt"));
        assertNull(tileSource.getStyleSource());
    }

    private static Stream<Arguments> testMapillaryStyle() {
        return Stream.of(Arguments.of(0, "Test Mapillary: mapillary-source", "https://tiles3.mapillary.com/v0.1/{z}/{x}/{y}.mvt"),
          Arguments.of(1, "Test Mapillary: mapillary-features-source",
            "https://a.mapillary.com/v3/map_features?tile={z}/{x}/{y}&client_id=_apiKey_"
              + "&layers=points&per_page=1000"),
          Arguments.of(2, "Test Mapillary: mapillary-traffic-signs-source",
            "https://a.mapillary.com/v3/map_features?tile={z}/{x}/{y}&client_id=_apiKey_"
              + "&layers=trafficsigns&per_page=1000"));
    }

    @ParameterizedTest
    @MethodSource("testMapillaryStyle")
    void testMapillaryStyle(Integer index, String expected, String dialogMockerText) {
        TestUtils.assumeWorkingJMockit();
        SelectLayerDialogMocker extendedDialogMocker = new SelectLayerDialogMocker();
        extendedDialogMocker.index = index;
        extendedDialogMocker.getMockResultMap().put(dialogMockerText, "Add layers");
        MapboxVectorTileSource tileSource = new MapboxVectorTileSource(
          new ImageryInfo("Test Mapillary", "file:/" + TestUtils.getTestDataRoot() + "mapillary.json"));
        MapboxVectorStyle styleSource = tileSource.getStyleSource();
        assertNotNull(styleSource);
        assertEquals(expected, tileSource.toString());
    }
}
