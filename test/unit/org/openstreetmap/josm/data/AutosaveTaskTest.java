// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.AutosaveTask.AutosaveLayerInfo;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * Unit tests for class {@link AutosaveTask}.
 */
public class AutosaveTaskTest {

    private static AutosaveTask task;

    /**
     * Setup test.
     * @throws IOException if autosave directory cannot be created
     */
    @BeforeClass
    public static void setUpBeforeClass() throws IOException {
        JOSMFixture.createUnitTestFixture().init(true);
        task = new AutosaveTask();
        Files.createDirectories(task.getAutosaveDir());
    }

    /**
     * Unit test to {@link AutosaveTask#getUnsavedLayersFiles} - empty case
     */
    @Test
    public void testGetUnsavedLayersFilesEmpty() {
        assertTrue(task.getUnsavedLayersFiles().isEmpty());
    }

    /**
     * Unit test to {@link AutosaveTask#getUnsavedLayersFiles} - non empty case
     * @throws IOException in case of I/O error
     */
    @Test
    public void testGetUnsavedLayersFilesNotEmpty() throws IOException {
        String autodir = task.getAutosaveDir().toString();
        File layer1 = Files.createFile(Paths.get(autodir, "layer1.osm")).toFile();
        File layer2 = Files.createFile(Paths.get(autodir, "layer2.osm")).toFile();
        File dir = Files.createDirectory(Paths.get(autodir, "dir.osm")).toFile();
        try {
            List<File> files = task.getUnsavedLayersFiles();
            assertEquals(2, files.size());
            assertTrue(files.contains(layer1));
            assertTrue(files.contains(layer2));
            assertFalse(files.contains(dir));
        } finally {
            Files.delete(dir.toPath());
            Files.delete(layer2.toPath());
            Files.delete(layer1.toPath());
        }
    }

    /**
     * Unit test to {@link AutosaveTask#getNewLayerFile}
     * @throws IOException in case of I/O error
     */
    @Test
    public void testGetNewLayerFile() throws IOException {
        AutosaveLayerInfo info = new AutosaveLayerInfo(new OsmDataLayer(new DataSet(), "layer", null));
        Calendar cal = Calendar.getInstance();
        cal.set(2016, 0, 1, 1, 2, 3);
        cal.set(Calendar.MILLISECOND, 456);
        Date fixed = cal.getTime();

        List<File> files = new ArrayList<>();

        try {
            for (int i = 0; i <= AutosaveTask.PROP_INDEX_LIMIT.get()+1; i++) {
                // Only retry 2 indexes to avoid 1000*1000 disk operations
                File f = task.getNewLayerFile(info, fixed, Math.max(0, i-2));
                files.add(f);
                if (i > AutosaveTask.PROP_INDEX_LIMIT.get()) {
                    assertNull(f);
                } else {
                    assertNotNull(f);
                    File pid = task.getPidFile(f);
                    assertTrue(pid.exists());
                    assertTrue(f.exists());
                    if (i == 0) {
                        assertEquals("null_20160101_010203456.osm", f.getName());
                        assertEquals("null_20160101_010203456.pid", pid.getName());
                    } else {
                        assertEquals("null_20160101_010203456_"+i+".osm", f.getName());
                        assertEquals("null_20160101_010203456_"+i+".pid", pid.getName());
                    }
                }
            }
        } finally {
            for (File f : files) {
                if (f != null) {
                    Files.delete(task.getPidFile(f).toPath());
                    Files.delete(f.toPath());
                }
            }
        }
    }
}
