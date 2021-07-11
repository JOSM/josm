// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.data.validation.tests.RightAngleBuildingTest;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link ValidatorErrorWriter}
 */
class ValidatorErrorWriterTest {

    /**
     * Setup rule
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    @Test
    void testEmpty() throws IOException {
        doTest(Collections.emptyList(), "");
    }

    @Test
    void testErrors() throws IOException {
        Locale.setDefault(Locale.ENGLISH);
        doTest(Arrays.asList(TestError.builder(new RightAngleBuildingTest(), Severity.OTHER, 3701)
                .message("Building with an almost square angle")
                .primitives(new Node(LatLon.NORTH_POLE)).build()),
                  "  <analyser timestamp='\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z' name='Almost right angle buildings'>"
                + "    <class id='1' level='3'>"
                + "      <classtext lang='en' title='Building with an almost square angle'/>"
                + "    </class>"
                + "    <error class='1'>"
                + "      <location lat='90\\.0' lon='0\\.0'/>"
                + "      <node id='-1' visible='true' lat='90\\.0' lon='0\\.0' />"
                + "      <text lang='en' value='null'/>"
                + "    </error>"
                + "  </analyser>");
    }

    private static void doTest(Collection<TestError> validationErrors, String content) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ValidatorErrorWriter writer = new ValidatorErrorWriter(baos)) {
            writer.write(validationErrors);
        }
        String xml = new String(baos.toByteArray(), StandardCharsets.UTF_8);
        assertTrue(xml.trim().replace("\r", "").replace("\n", "")
                .matches("<\\?xml version='1.0' encoding='UTF-8'\\?>" +
                        "<analysers generator='JOSM' timestamp='\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z'>" +
                        content +
                        "</analysers>"), xml);
    }
}
