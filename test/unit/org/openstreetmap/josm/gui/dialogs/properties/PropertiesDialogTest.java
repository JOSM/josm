// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.PrimitiveHoverListener;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.tools.ReflectionUtils;

/**
 * Unit tests of {@link PropertiesDialog} class.
 */
@BasicPreferences
class PropertiesDialogTest {
    @RegisterExtension
    static JOSMTestRules rules = new JOSMTestRules().main().projection();

    private static String createSearchSetting(List<OsmPrimitive> sel, boolean sameType) {
        return PropertiesDialog.createSearchSetting("foo", sel, sameType).text;
    }

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/12504">#12504</a>.
     */
    @Test
    void testTicket12504() {
        List<OsmPrimitive> sel = new ArrayList<>();
        // 160 objects with foo=bar, 400 objects without foo
        for (int i = 0; i < 160+400; i++) {
            Node n = new Node(LatLon.ZERO);
            if (i < 160) {
                n.put("foo", "bar");
            }
            sel.add(n);
        }
        assertEquals("(\"foo\"=\"bar\")", createSearchSetting(sel, false));

        Node n = new Node(LatLon.ZERO);
        n.put("foo", "baz");
        sel.add(0, n);

        assertEquals("(\"foo\"=\"baz\") OR (\"foo\"=\"bar\")", createSearchSetting(sel, false));

        sel.remove(0);

        Way w = new Way();
        w.put("foo", "bar");
        sel.add(0, w);

        assertEquals("(type:way \"foo\"=\"bar\") OR (type:node \"foo\"=\"bar\")", createSearchSetting(sel, true));
    }

    static Stream<Arguments> testTicket22487() {
        return Stream.of(
                Arguments.of("Layer add", (Runnable) () ->
                        MainApplication.getLayerManager().addLayer(new OsmDataLayer(new DataSet(), "testTicket22487-layerAdd", null))),
                Arguments.of("Layer hide", (Runnable) () -> {
                    // We need to toggle the layer visibility to hit the bug.
                    MainApplication.getLayerManager().getLayers().forEach(layer -> layer.setVisible(false));
                    MainApplication.getLayerManager().getLayers().forEach(layer -> layer.setVisible(true));
                })
        );
    }

    @ParameterizedTest
    @MethodSource
    void testTicket22487(String ignored_title, Runnable action) throws ReflectiveOperationException {
        Field primitiveHoverListenersField = NavigatableComponent.class.getDeclaredField("primitiveHoverListeners");
        ReflectionUtils.setObjectsAccessible(primitiveHoverListenersField);

        DataSet ds = new DataSet();
        OsmDataLayer layer = new OsmDataLayer(ds, "testTicket22487", null);
        // Ensure that the navigatable component is set up
        MainApplication.getLayerManager().addLayer(layer);
        PropertiesDialog propertiesDialog = MainApplication.getMap().propertiesDialog;
        @SuppressWarnings("unchecked")
        CopyOnWriteArrayList<PrimitiveHoverListener> listeners =
                (CopyOnWriteArrayList<PrimitiveHoverListener>) primitiveHoverListenersField.get(MainApplication.getMap().mapView);
        assertTrue(listeners.contains(propertiesDialog));

        // Set the properties to false
        PropertiesDialog.PROP_PREVIEW_ON_HOVER.put(false);
        assertFalse(listeners.contains(propertiesDialog));

        action.run();
        assertFalse(listeners.contains(propertiesDialog));
    }
}
