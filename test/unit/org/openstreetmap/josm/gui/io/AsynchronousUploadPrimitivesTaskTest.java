// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import java.util.Collections;
import java.util.Optional;

import javax.swing.JOptionPane;

import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.APIDataSet;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.UploadStrategySpecification;
import org.openstreetmap.josm.testutils.annotations.AssertionsInEDT;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.HTTP;
import org.openstreetmap.josm.testutils.mockers.JOptionPaneSimpleMocker;

/**
 * Unit tests of {@link AsynchronousUploadPrimitivesTask}.
 */
@AssertionsInEDT
@BasicPreferences
@HTTP
class AsynchronousUploadPrimitivesTaskTest {

    private UploadStrategySpecification strategy;
    private OsmDataLayer layer;
    private APIDataSet toUpload;
    private Changeset changeset;
    private AsynchronousUploadPrimitivesTask uploadPrimitivesTask;

    /**
     * Bootstrap.
     */
    @BeforeEach
    public void bootStrap() {
        DataSet dataSet = new DataSet();
        Node node1 = new Node();
        Node node2 = new Node();
        node1.setCoor(new LatLon(0, 0));
        node2.setCoor(new LatLon(30, 30));
        Way way = new Way();
        way.addNode(node1);
        way.addNode(node2);
        dataSet.addPrimitive(node1);
        dataSet.addPrimitive(node2);
        dataSet.addPrimitive(way);

        toUpload = new APIDataSet(dataSet);
        layer = new OsmDataLayer(dataSet, "uploadTest", null);
        strategy = new UploadStrategySpecification();
        changeset = new Changeset();
        uploadPrimitivesTask = AsynchronousUploadPrimitivesTask.createAsynchronousUploadTask(strategy, layer, toUpload, changeset).get();
    }

    /**
     * Tear down.
     */
    @AfterEach
    public void tearDown() {
        toUpload = null;
        layer = null;
        strategy = null;
        changeset = null;
        if (uploadPrimitivesTask != null) {
            uploadPrimitivesTask.cancel();
        }
        uploadPrimitivesTask = null;
    }

    /**
     * Test single upload instance.
     */
    @Test
    void testSingleUploadInstance() {
        TestUtils.assumeWorkingJMockit();
        new JOptionPaneSimpleMocker(Collections.singletonMap(
                "A background upload is already in progress. Kindly wait for it to finish before uploading new changes", JOptionPane.OK_OPTION
            ));
        Optional<AsynchronousUploadPrimitivesTask> task = AsynchronousUploadPrimitivesTask.
                createAsynchronousUploadTask(strategy, layer, toUpload, changeset);
        Assert.assertNotNull(uploadPrimitivesTask);
        Assert.assertFalse(task.isPresent());
    }
}
