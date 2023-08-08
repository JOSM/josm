// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.PerformanceTestUtils;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.validation.tests.ApiCapabilitiesTest;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.Projection;
import org.openstreetmap.josm.testutils.annotations.Territories;

/**
 * Performance test of {@code ValidationTask}.
 */
@BasicPreferences
@Projection
@Territories
class ValidationTaskPerformanceTest {

    private List<org.openstreetmap.josm.data.validation.Test> tests;

    /**
     * Setup test.
     *
     * @throws Exception if any error occurs
     */
    @BeforeEach
    void setUp() throws Exception {
        tests = OsmValidator.getTests().stream().filter(test -> !(test instanceof ApiCapabilitiesTest)).collect(Collectors.toList());
        OsmValidator.initialize();
        OsmValidator.initializeTests(tests);

        DataSet dataSet = PerformanceTestUtils.getNeubrandenburgDataSet();
        // some tests obtain the active dataset
        MainApplication.getLayerManager().addLayer(new OsmDataLayer(dataSet, dataSet.getName(), null));
    }

    /**
     * Runs the validation task on a test dataset.
     */
    @Test
    void test() {
        DataSet dataSet = MainApplication.getLayerManager().getActiveDataSet();
        Collection<OsmPrimitive> primitives = dataSet.allPrimitives();

        PerformanceTestUtils.runPerformanceTest("ValidationTask#realRun on " + dataSet.getName(), () -> {
            ValidationTask validationTask = new ValidationTask(NullProgressMonitor.INSTANCE, tests, primitives, primitives);
            validationTask.realRun();
            assertTrue(validationTask.getErrors().size() > 3000);
        });
    }
}
