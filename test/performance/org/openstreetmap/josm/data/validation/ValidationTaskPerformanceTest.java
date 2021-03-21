// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.PerformanceTestUtils;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.validation.tests.ApiCapabilitiesTest;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Performance test of {@code ValidationTask}.
 */
class ValidationTaskPerformanceTest {

    private List<org.openstreetmap.josm.data.validation.Test> tests;

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().projection().territories().preferences();

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
