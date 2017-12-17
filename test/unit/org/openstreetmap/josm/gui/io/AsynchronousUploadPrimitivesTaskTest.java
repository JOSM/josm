// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import java.util.Optional;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.APIDataSet;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.UploadStrategySpecification;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link AsynchronousUploadPrimitivesTask}.
 */
public class AsynchronousUploadPrimitivesTaskTest {

    private UploadStrategySpecification strategy;
    private OsmDataLayer layer;
    private APIDataSet toUpload;
    private Changeset changeset;
    private AsynchronousUploadPrimitivesTask uploadPrimitivesTask;

    /**
     * Setup tests
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    @Before
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

    @After
    public void tearDown() {
        toUpload = null;
        layer = null;
        strategy = null;
        changeset = null;
        uploadPrimitivesTask.cancel();
        uploadPrimitivesTask = null;
    }

    @Test
    public void testSingleUploadInstance() {
        Optional<AsynchronousUploadPrimitivesTask> task = AsynchronousUploadPrimitivesTask.
                createAsynchronousUploadTask(strategy, layer, toUpload, changeset);
        Assert.assertNotNull(uploadPrimitivesTask);
        Assert.assertFalse(task.isPresent());
    }
}
