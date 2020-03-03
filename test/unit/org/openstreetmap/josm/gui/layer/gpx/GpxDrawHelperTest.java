// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.gpx;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.gpx.GpxDrawHelper.ColorMode;
import org.openstreetmap.josm.io.GpxReaderTest;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.ColorHelper;
import org.xml.sax.SAXException;

import com.google.common.collect.ImmutableMap;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link GpxDrawHelper} class.
 */
public class GpxDrawHelperTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/12312">#12312</a>.
     * @throws IOException if any I/O error occurs
     * @throws FileNotFoundException if the data file is not found
     * @throws SAXException if any SAX error occurs
     */
    @Test
    public void testTicket12312() throws FileNotFoundException, IOException, SAXException {
        final List<String> colors = calculateColors(TestUtils.getRegressionDataFile(12312, "single_trackpoint.gpx"),
                ImmutableMap.of("colormode.dynamic-range", "true",
                        "colormode", Integer.toString(ColorMode.VELOCITY.toIndex())),
                1);
        assertEquals("[null]", colors.toString());
    }

    /**
     * Tests coloring of an example track using the default color.
     * @throws IOException if any I/O error occurs
     * @throws FileNotFoundException if the data file is not found
     * @throws SAXException if any SAX error occurs
     */
    @Test
    public void testNone() throws IOException, SAXException {
        final List<String> colors = calculateColors("nodist/data/2094047.gpx", ImmutableMap.of(), 10);
        assertEquals("[#000000, #000000, #000000, #000000, #000000, #000000, #000000, #000000, #000000, #000000]", colors.toString());
    }

    /**
     * Tests coloring of an example track using its velocity.
     * @throws IOException if any I/O error occurs
     * @throws FileNotFoundException if the data file is not found
     * @throws SAXException if any SAX error occurs
     */
    @Test
    public void testVelocity() throws IOException, SAXException {
        final List<String> colors = calculateColors("nodist/data/2094047.gpx",
                ImmutableMap.of("colormode", Integer.toString(ColorMode.VELOCITY.toIndex())), 10);
        assertEquals("[#000000, #FFAD00, #FFA800, #FFA800, #FF9E00, #FF9400, #FF7000, #FF7000, #FF8000, #FF9400]", colors.toString());
    }

    /**
     * Tests coloring of an example track using its velocity with a dynamic scale
     * @throws IOException if any I/O error occurs
     * @throws FileNotFoundException if the data file is not found
     * @throws SAXException if any SAX error occurs
     */
    @Test
    public void testVelocityDynamic() throws IOException, SAXException {
        final List<String> colors = calculateColors("nodist/data/2094047.gpx",
                ImmutableMap.of("colormode.dynamic-range", "true",
                        "colormode", Integer.toString(ColorMode.VELOCITY.toIndex())),
                10);
        assertEquals("[#000000, #00FFE0, #00FFC2, #00FFC2, #00FF75, #00FF3D, #99FF00, #94FF00, #38FF00, #00FF38]", colors.toString());
    }

    /**
     * Tests coloring of an example track using its direction.
     * @throws IOException if any I/O error occurs
     * @throws FileNotFoundException if the data file is not found
     * @throws SAXException if any SAX error occurs
     */
    @Test
    public void testDirection() throws IOException, SAXException {
        final List<String> colors = calculateColors("nodist/data/2094047.gpx",
                ImmutableMap.of("colormode", Integer.toString(ColorMode.DIRECTION.toIndex())), 10);
        assertEquals("[#000000, #EAEC25, #EDEA26, #EDE525, #ECD322, #EBB81D, #E85A0D, #E73708, #E84D0B, #EA8A15]", colors.toString());
    }

    /**
     * Tests coloring of an example track using its direction.
     * @throws IOException if any I/O error occurs
     * @throws FileNotFoundException if the data file is not found
     * @throws SAXException if any SAX error occurs
     */
    @Test
    public void testTime() throws IOException, SAXException {
        final List<String> colors = calculateColors("nodist/data/2094047.gpx",
                ImmutableMap.of("colormode", Integer.toString(ColorMode.TIME.toIndex())), 10);
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
