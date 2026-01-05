// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.gpx.TimeSource;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.ThreadSync;
import org.openstreetmap.josm.tools.date.DateUtils;

/**
 * Unit tests of {@link GeoImageLayer} class.
 */
// Basic preferences are needed for OSM primitives
@BasicPreferences
@Main
class GeoImageLayerTest {
    @AfterEach
    void tearDown() {
        // We need to ensure that all threads are "done" before continuing.
        // Otherwise, other tests may have an ImageViewerDialog that causes issues...
        // Note: we cannot (currently) use the ThreadSync annotation since it runs
        // *after* local AfterEach and AfterAll methods.
        new ThreadSync.ThreadSyncExtension().threadSync();
        if (ImageViewerDialog.hasInstance()) {
            ImageViewerDialog.getInstance().destroy();
        }
    }

    /**
     * Test that {@link GeoImageLayer#mergeFrom} throws IAE for invalid arguments
     */
    @Test
    void testMergeFromIAE() {
        GeoImageLayer geoImageLayer = new GeoImageLayer(Collections.emptyList(), null);
        OsmDataLayer osmDataLayer = new OsmDataLayer(new DataSet(), "", null);
        assertThrows(IllegalArgumentException.class, () -> geoImageLayer.mergeFrom(osmDataLayer));
    }

    /**
     * Test that {@link GeoImageLayer#getSortedImgList} filters images without ExifGpsTime
     */
    @Test
    void testMissingGPSTimeStamp() {
        ImageEntry i1, i2;
        i1 = new ImageEntry();
        i1.setExifGpsTime(DateUtils.parseInstant("2016:01:03 12:00:00"));
        i2 = new ImageEntry();
        i2.setExifTime(DateUtils.parseInstant("2016:01:03 12:00:01"));

        GeoImageLayer geoGpsImageLayer = new GeoImageLayer(Arrays.asList(i1, i2), null);
        assertEquals(1, geoGpsImageLayer.getSortedImgList(false, false, TimeSource.EXIFGPSTIME).size());
    }

}
