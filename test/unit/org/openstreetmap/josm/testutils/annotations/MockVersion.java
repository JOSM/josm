// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.testutils.JOSMTestRules;

/**
 * This is only publicly available for {@link JOSMTestRules#assumeRevision(String)}.
 * This may (and probably will) become a private class in the future.
 */
public class MockVersion extends Version {
    public MockVersion(final String propertiesString) {
        super.initFromRevisionInfo(new ByteArrayInputStream(propertiesString.getBytes(StandardCharsets.UTF_8)));
    }
}
