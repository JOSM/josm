// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.io.Compression;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import java.io.File;
import java.io.InputStream;

/**
 * Performance test of {@code MapCSSTagChecker}.
 */
class MapCSSTagCheckerPerformanceTest {

    private MapCSSTagChecker tagChecker;
    private DataSet dsCity;

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
        tagChecker = new MapCSSTagChecker();
        tagChecker.initialize();
        try (InputStream in = Compression.getUncompressedFileInputStream(new File("nodist/data/neubrandenburg.osm.bz2"))) {
            dsCity = OsmReader.parseDataSet(in, NullProgressMonitor.INSTANCE);
        }
    }

    @Test
    void testCity() {
        tagChecker.visit(dsCity.allPrimitives());
    }
}
