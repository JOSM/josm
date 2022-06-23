// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.gui.layer.TMSLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests for class {@link SessionLoadAction}.
 */
@BasicPreferences
class SessionLoadActionTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().main().projection();

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/17702">Bug #17702</a>.
     */
    @Test
    void testTicket17702() {
        assertFalse(SessionLoadAction.Loader.addLayer(new TMSLayer(new ImageryInfo(
                "Bing Карта (GLOBALCITY)",
                "http://ecn.dynamic.t{switch:1,2,3}.tiles.virtualearth.net/comp/ch/{$q}?mkt=en-us&it=G,VE,BX,L,LA&shading=hill&og=2&n=z",
                ImageryType.TMS.getTypeString(), null, null))));
    }
}

