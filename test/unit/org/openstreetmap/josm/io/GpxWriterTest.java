// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.function.Consumer;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxConstants;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Tests the {@link GpxWriter}.
 */
public class GpxWriterTest {

    /**
     * Setup rule
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    private static void testSingleWaypoint(Consumer<WayPoint> consumer, String atts) throws IOException {
        GpxData gpx = new GpxData();
        WayPoint waypoint = new WayPoint(LatLon.ZERO);
        consumer.accept(waypoint);
        gpx.addWaypoint(waypoint);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GpxWriter writer = new GpxWriter(baos)) {
            writer.write(gpx);
        }
        assertEquals(String.format("<?xml version='1.0' encoding='UTF-8'?>%n" +
                "<gpx version=\"1.1\" creator=\"JOSM GPX export\" xmlns=\"http://www.topografix.com/GPX/1/1\"%n" +
                "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"%n" +
                "    xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">%n" +
                "  <metadata>%n" +
                "    <bounds minlat=\"0.0\" minlon=\"0.0\" maxlat=\"0.0\" maxlon=\"0.0\"/>%n" +
                "  </metadata>%n" +
                "  <wpt lat=\"0.0\" lon=\"0.0\">%n" +
                atts +
                "  </wpt>%n" +
                "</gpx>"), baos.toString(StandardCharsets.UTF_8.name()));
    }

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/16550">#16550</a>
     * @throws IOException never
     */
    @Test
    public void testTicket16550() throws IOException {
        // Checks that time stored as date is correctly written into XML timestamp
        testSingleWaypoint(
                w -> w.put(GpxConstants.PT_TIME, Date.from(LocalDate.of(2018, Month.AUGUST, 2).atStartOfDay(ZoneOffset.UTC).toInstant())),
                "    <time>2018-08-02T00:00:00Z</time>%n");
    }

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/16725">#16725</a>
     * @throws IOException never
     */
    @Test
    public void testTicket16725() throws IOException {
        // Checks that sat, hdop, pdop, vdop are correctly exported
        testSingleWaypoint(
                w -> {
                    w.put(GpxConstants.PT_SAT, 16);
                    w.put(GpxConstants.PT_HDOP, 0.7);
                    w.put(GpxConstants.PT_VDOP, 0.9);
                    w.put(GpxConstants.PT_PDOP, 1.2);
                },
                "    <sat>16</sat>%n" +
                "    <hdop>0.7</hdop>%n" +
                "    <vdop>0.9</vdop>%n" +
                "    <pdop>1.2</pdop>%n");
    }
}
