// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Integration tests for the {@link Tag2Link}
 */
public class Tag2LinkTestIT {

    /**
     * Setup rule
     */
    @ClassRule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public static JOSMTestRules test = new JOSMTestRules().timeout(20_000);

    /**
     * Integration test of function {@link org.openstreetmap.josm.tools.Tag2Link#initialize()}.
     */
    @Test
    public void testInitialize() {
        Tag2Link.initialize();
        Assert.assertTrue("obtails at least 40 rules", Tag2Link.wikidataRules.size() > 40);
    }
}
