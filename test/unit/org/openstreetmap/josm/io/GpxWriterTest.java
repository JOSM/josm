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

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/16550">#16550</a>
     * @throws IOException never
     */
    @Test
    public void testTicket16550() throws IOException {
        GpxData gpx = new GpxData();
        WayPoint waypoint = new WayPoint(LatLon.ZERO);
        waypoint.put(GpxConstants.PT_TIME, Date.from(LocalDate.of(2018, Month.AUGUST, 2).atStartOfDay(ZoneOffset.UTC).toInstant()));
        gpx.addWaypoint(waypoint);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GpxWriter writer = new GpxWriter(baos)) {
            writer.write(gpx);
        }
        // Checks that time stored as date is correctly written into XML timestamp
        assertEquals(String.format("<?xml version='1.0' encoding='UTF-8'?>%n" +
                "<gpx version=\"1.1\" creator=\"JOSM GPX export\" xmlns=\"http://www.topografix.com/GPX/1/1\"%n" +
                "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"%n" +
                "    xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">%n" +
                "  <metadata>%n" +
                "    <bounds minlat=\"0.0\" minlon=\"0.0\" maxlat=\"0.0\" maxlon=\"0.0\"/>%n" +
                "  </metadata>%n" +
                "  <wpt lat=\"0.0\" lon=\"0.0\">%n" +
                "    <time>2018-08-02T02:00:00.000Z</time>%n" +
                "  </wpt>%n" +
                "</gpx>"), baos.toString(StandardCharsets.UTF_8.name()));
    }
}
