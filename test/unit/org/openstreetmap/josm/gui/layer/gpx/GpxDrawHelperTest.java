// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.gpx;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.gpx.GpxDrawHelper.ColorMode;
import org.openstreetmap.josm.io.GpxReaderTest;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.tools.ColorHelper;
import org.xml.sax.SAXException;

/**
 * Unit tests of {@link GpxDrawHelper} class.
 */
@BasicPreferences
class GpxDrawHelperTest {
    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/12312">#12312</a>.
     * @throws IOException if any I/O error occurs
     * @throws FileNotFoundException if the data file is not found
     * @throws SAXException if any SAX error occurs
     */
    @Test
    void testTicket12312() throws FileNotFoundException, IOException, SAXException {
        final Map<String, String> prefs = new HashMap<String, String>() {{
            put("colormode.dynamic-range", "true");
            put("colormode", Integer.toString(ColorMode.VELOCITY.toIndex()));
        }};
        final List<String> colors = calculateColors(TestUtils.getRegressionDataFile(12312, "single_trackpoint.gpx"), prefs, 1);
        assertEquals("[null]", colors.toString());
    }

    /**
     * Tests coloring of an example track using the default color.
     * @throws IOException if any I/O error occurs
     * @throws FileNotFoundException if the data file is not found
     * @throws SAXException if any SAX error occurs
     */
    @Test
    void testNone() throws IOException, SAXException {
        final List<String> colors = calculateColors("nodist/data/2094047.gpx", Collections.emptyMap(), 10);
        assertEquals("[#000000, #000000, #000000, #000000, #000000, #000000, #000000, #000000, #000000, #000000]", colors.toString());
    }

    /**
     * Tests coloring of an example track using its velocity.
     * @throws IOException if any I/O error occurs
     * @throws FileNotFoundException if the data file is not found
     * @throws SAXException if any SAX error occurs
     */
    @Test
    void testVelocity() throws IOException, SAXException {
        final Map<String, String> prefs = Collections.singletonMap("colormode", Integer.toString(ColorMode.VELOCITY.toIndex()));
        final List<String> colors = calculateColors("nodist/data/2094047.gpx", prefs, 10);
        assertEquals("[#000000, #FFAD00, #FFA800, #FFA800, #FF9E00, #FF9400, #FF7000, #FF7000, #FF8000, #FF9400]", colors.toString());
    }

    /**
     * Tests coloring of an example track using its velocity with a dynamic scale
     * @throws IOException if any I/O error occurs
     * @throws FileNotFoundException if the data file is not found
     * @throws SAXException if any SAX error occurs
     */
    @Test
    void testVelocityDynamic() throws IOException, SAXException {
        final Map<String, String> prefs = new HashMap<String, String>() {{
            put("colormode.dynamic-range", "true");
            put("colormode", Integer.toString(ColorMode.VELOCITY.toIndex()));
        }};
        final List<String> colors = calculateColors("nodist/data/2094047.gpx", prefs, 10);
        assertEquals("[#000000, #00FFE0, #00FFC2, #00FFC2, #00FF75, #00FF3D, #99FF00, #94FF00, #38FF00, #00FF38]", colors.toString());
    }

    /**
     * Tests coloring of an example track using its direction.
     * @throws IOException if any I/O error occurs
     * @throws FileNotFoundException if the data file is not found
     * @throws SAXException if any SAX error occurs
     */
    @Test
    void testDirection() throws IOException, SAXException {
        final Map<String, String> prefs = Collections.singletonMap("colormode", Integer.toString(ColorMode.DIRECTION.toIndex()));
        final List<String> colors = calculateColors("nodist/data/2094047.gpx", prefs, 10);
        assertEquals("[#000000, #EAEC25, #EDEA26, #EDE525, #ECD322, #EBB81D, #E85A0D, #E73708, #E84D0B, #EA8A15]", colors.toString());
    }

    /**
     * Tests coloring of an example track using its direction.
     * @throws IOException if any I/O error occurs
     * @throws FileNotFoundException if the data file is not found
     * @throws SAXException if any SAX error occurs
     */
    @Test
    void testTime() throws IOException, SAXException {
        final Map<String, String> prefs = Collections.singletonMap("colormode", Integer.toString(ColorMode.TIME.toIndex()));
        final List<String> colors = calculateColors("nodist/data/2094047.gpx", prefs, 10);
        assertEquals("[#000000, #FF0000, #FF0000, #FF0500, #FF0500, #FF0A00, #FF0A00, #FF1F00, #FF2E00, #FF3300]", colors.toString());
    }

    /**
     *
     * @param fileName the GPX filename to parse
     * @param layerPrefs a HashMap representing the layer specific preferences
     * @param n the number of waypoints of the first track/segment to analyze
     * @return the HTML color codes for the first {@code n} points
     * @throws IOException if any I/O error occurs
     * @throws FileNotFoundException if the data file is not found
     * @throws SAXException if any SAX error occurs
     */
    static List<String> calculateColors(String fileName, Map<String, String> layerPrefs, int n) throws IOException, SAXException {
        final GpxData data = GpxReaderTest.parseGpxData(fileName);
        data.getLayerPrefs().putAll(layerPrefs);
        final GpxLayer layer = new GpxLayer(data);
        final GpxDrawHelper gdh = new GpxDrawHelper(layer);
        gdh.readPreferences();
        gdh.calculateColors();
        return data.getTrackPoints().limit(n).map(p -> ColorHelper.color2html(p.customColoring)).collect(Collectors.toList());
    }
}
