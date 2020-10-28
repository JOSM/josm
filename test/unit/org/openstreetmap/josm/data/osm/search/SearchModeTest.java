// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.search;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link SearchMode}.
 */
class SearchModeTest {

    /**
     * Setup rules.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test of {@link SearchMode} enum.
     */
    @Test
    void testEnumSearchMode() {
        TestUtils.superficialEnumCodeCoverage(SearchMode.class);
    }
}
