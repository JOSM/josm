// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.gui.layer.AutosaveTask.AutosaveLayerInfo;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link AutosaveTask}.
 */
public class AutosaveTaskTest {
    /**
     * We need preferences and a home directory for this.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().platform().projection();

    private AutosaveTask task;

    /**
     * Setup test.
     * @throws IOException if autosave directory cannot be created
     */
    @Before
    public void setUp() throws IOException {
        task = new AutosaveTask();
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
        Files.createDirectories(task.getAutosaveDir());
        String autodir = task.getAutosaveDir().toString();
        File layer1 = Files.createFile(Paths.get(autodir, "layer1.osm")).toFile();
        File layer2 = Files.createFile(Paths.get(autodir, "layer2.osm")).toFile();
        File dir = Files.createDirectory(Paths.get(autodir, "dir.osm")).toFile();
        List<File> files = task.getUnsavedLayersFiles();
        assertEquals(2, files.size());
        assertTrue(files.contains(layer1));
        assertTrue(files.contains(layer2));
        assertFalse(files.contains(dir));
    }

    /**
     * Unit test to {@link AutosaveTask#getNewLayerFile}
     * @throws IOException in case of I/O error
     */
    @Test
    public void testGetNewLayerFile() throws IOException {
        Files.createDirectories(task.getAutosaveDir());
        AutosaveLayerInfo info = new AutosaveLayerInfo(new OsmDataLayer(new DataSet(), "layer", null));
        Date fixed = Date.from(ZonedDateTime.of(2016, 1, 1, 1, 2, 3, 456_000_000, ZoneId.systemDefault()).toInstant());

        AutosaveTask.PROP_INDEX_LIMIT.put(5);
        for (int i = 0; i <= AutosaveTask.PROP_INDEX_LIMIT.get() + 2; i++) {
            // Only retry 2 indexes to avoid 1000*1000 disk operations
            File f = task.getNewLayerFile(info, fixed, Math.max(0, i - 2));
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
                    assertEquals("null_20160101_010203456_" + i + ".osm", f.getName());
                    assertEquals("null_20160101_010203456_" + i + ".pid", pid.getName());
                }
            }
        }
    }

    /**
     * Tests if {@link AutosaveTask#schedule()} creates the directories.
     */
    @Test
    public void testScheduleCreatesDirectories() {
        try {
            task.schedule();
            assertTrue(task.getAutosaveDir().toFile().isDirectory());
        } finally {
            task.cancel();
        }
    }

    /**
     * Tests that {@link AutosaveTask#run()} saves every layer
     */
    @Test
    public void testAutosaveIgnoresUnmodifiedLayer() {
        OsmDataLayer layer = new OsmDataLayer(new DataSet(), "OsmData", null);
        Main.getLayerManager().addLayer(layer);
        try {
            task.schedule();
            assertEquals(0, countFiles());
            task.run();
            assertEquals(0, countFiles());
        } finally {
            task.cancel();
        }
    }

    private int countFiles() {
        String[] files = task.getAutosaveDir().toFile().list((dir, name) -> name.endsWith(".osm"));
        return files != null ? files.length : 0;
    }

    /**
     * Tests that {@link AutosaveTask#run()} saves every layer.
     */
    @Test
    public void testAutosaveSavesLayer() {
        runAutosaveTaskSeveralTimes(1);
    }

    /**
     * Tests that {@link AutosaveTask#run()} saves every layer.
     */
    @Test
    public void testAutosaveSavesLayerMultipleTimes() {
        AutosaveTask.PROP_FILES_PER_LAYER.put(3);
        runAutosaveTaskSeveralTimes(5);
    }

    private void runAutosaveTaskSeveralTimes(int times) {
        DataSet data = new DataSet();
        OsmDataLayer layer = new OsmDataLayer(data, "OsmData", null);
        Main.getLayerManager().addLayer(layer);
        try {
            task.schedule();
            assertEquals(0, countFiles());

            for (int i = 0; i < times; i++) {
                data.addPrimitive(new Node(new LatLon(10, 10)));
                task.run();
                assertEquals(Math.min(i + 1, 3), countFiles());
            }

        } finally {
            task.cancel();
        }
    }

    /**
     * Tests that {@link AutosaveTask#discardUnsavedLayers()} ignores layers from the current instance
     * @throws IOException in case of I/O error
     */
    @Test
    public void testDiscardUnsavedLayersIgnoresCurrentInstance() throws IOException {
        runAutosaveTaskSeveralTimes(1);
        try (BufferedWriter file = Files.newBufferedWriter(
                new File(task.getAutosaveDir().toFile(), "any_other_file.osm").toPath(), StandardCharsets.UTF_8)) {
            file.append("");
        }
        assertEquals(2, countFiles());

        task.discardUnsavedLayers();
        assertEquals(1, countFiles());
    }

    /**
     * Tests that {@link AutosaveTask#run()} handles duplicate layers
     */
    @Test
    public void testAutosaveHandlesDupplicateNames() {
        DataSet data1 = new DataSet();
        OsmDataLayer layer1 = new OsmDataLayer(data1, "OsmData", null);
        Main.getLayerManager().addLayer(layer1);

        DataSet data2 = new DataSet();
        OsmDataLayer layer2 = new OsmDataLayer(data2, "OsmData", null);

        try {
            task.schedule();
            assertEquals(0, countFiles());
            // also test adding layer later
            Main.getLayerManager().addLayer(layer2);

            data1.addPrimitive(new Node(new LatLon(10, 10)));
            data2.addPrimitive(new Node(new LatLon(10, 10)));
            task.run();
            assertEquals(2, countFiles());
        } finally {
            task.cancel();
        }
    }

    /**
     * Test that {@link AutosaveTask#recoverUnsavedLayers()} recovers unsaved layers.
     * @throws Exception in case of error
     */
    @Test
    public void testRecoverLayers() throws Exception {
        runAutosaveTaskSeveralTimes(1);
        try (BufferedWriter file = Files.newBufferedWriter(
                new File(task.getAutosaveDir().toFile(), "any_other_file.osm").toPath(), StandardCharsets.UTF_8)) {
            file.append("<?xml version=\"1.0\"?><osm version=\"0.6\"><node id=\"1\" lat=\"1\" lon=\"2\" version=\"1\"/></osm>");
        }

        assertEquals(2, countFiles());
        task.recoverUnsavedLayers().get();

        assertEquals(1, countFiles());
    }
}
