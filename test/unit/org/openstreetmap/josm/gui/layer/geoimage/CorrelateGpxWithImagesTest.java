// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.io.GpxReaderTest;

public class CorrelateGpxWithImagesTest {

    @Test
    public void testMatchGpxTrack() throws Exception {
        final GpxData munich = GpxReaderTest.parseGpxData("data_nodist/munich.gpx");
        final ImageEntry i1 = new ImageEntry();
        i1.setExifGpsTime();
        CorrelateGpxWithImages.matchGpxTrack(null, munich, 0);
    }
}
