// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.openstreetmap.josm.data.osm.DataSet.DownloadPolicy;
import org.openstreetmap.josm.data.osm.DataSet.UploadPolicy;
import org.openstreetmap.josm.data.osm.NodeData;

/**
 * Unit tests of {@link OsmWriter} class.
 */
public class OsmWriterTest {

    /**
     * Unit test of {@link OsmWriter#byIdComparator}.
     */
    @Test
    public void testByIdComparator() {

        final List<NodeData> ids = new ArrayList<>();
        for (Long id : Arrays.asList(12L, Long.MIN_VALUE, 65L, -12L, 2L, 0L, -3L, -20L, Long.MAX_VALUE)) {
            final NodeData n = new NodeData();
            n.setId(id);
            ids.add(n);
        }

        Collections.sort(ids, OsmWriter.byIdComparator);

        final long[] longIds = ids.stream().mapToLong(NodeData::getUniqueId).toArray();
        assertArrayEquals(new long[] {
                -3, -12, -20, -9223372036854775808L, 0, 2, 12, 65, 9223372036854775807L
        }, longIds);
    }

    /**
     * Unit test of {@link OsmWriter#header(DownloadPolicy, UploadPolicy)}.
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testHeader() throws IOException {
        doTestHeader(null, null,
                "<osm version='0.6' generator='JOSM'>");
        doTestHeader(DownloadPolicy.NORMAL, UploadPolicy.NORMAL,
                "<osm version='0.6' generator='JOSM'>");
        doTestHeader(DownloadPolicy.BLOCKED, UploadPolicy.BLOCKED,
                "<osm version='0.6' download='never' upload='never' generator='JOSM'>");
    }

    private static void doTestHeader(DownloadPolicy download, UploadPolicy upload, String expected) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter out = new PrintWriter(baos);
             OsmWriter writer = OsmWriterFactory.createOsmWriter(out, true, OsmWriter.DEFAULT_API_VERSION)) {
            writer.header(download, upload);
        }
        assertEquals("<?xml version='1.0' encoding='UTF-8'?>" + expected,
                baos.toString("UTF-8").replaceAll("\r", "").replaceAll("\n", ""));
    }
}
