// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.Assert.assertFalse;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.gui.layer.TMSLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link SessionLoadAction}.
 */
public class SessionLoadActionTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().main().projection();

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/17702">Bug #17702</a>.
     */
    @Test
    public void testTicket17702() {
        assertFalse(SessionLoadAction.Loader.addLayer(new TMSLayer(new ImageryInfo(
                "Bing Карта (GLOBALCITY)",
                "http://ecn.dynamic.t{switch:1,2,3}.tiles.virtualearth.net/comp/ch/{$q}?mkt=en-us&it=G,VE,BX,L,LA&shading=hill&og=2&n=z",
                ImageryType.TMS.getTypeString(), null, null))));
    }
}
