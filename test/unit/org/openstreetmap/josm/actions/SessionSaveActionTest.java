// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.session.SessionWriterTest;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.testutils.mockers.JOptionPaneSimpleMocker;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link SessionSaveAsAction}.
 */
class SessionSaveActionTest {
    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().main().projection();

    /**
     * Unit test of {@link SessionSaveAction}
     * @throws IOException Temp file could not be created
     */
    @Test
    void testSaveAction() throws IOException {
        TestUtils.assumeWorkingJMockit();

        File jos = File.createTempFile("session", ".jos");
        File joz = new File(jos.getAbsolutePath().replaceFirst(".jos$", ".joz"));
        assertTrue(jos.exists());
        assertFalse(joz.exists());

        String overrideStr = "javax.swing.JLabel[,0,0,0x0,invalid,alignmentX=0.0,alignmentY=0.0,border=,flags=8388608,maximumSize=,minimumSize=,"
                + "preferredSize=,defaultIcon=,disabledIcon=,horizontalAlignment=LEADING,horizontalTextPosition=TRAILING,iconTextGap=4,"
                + "labelFor=,text=<html>The following layer has been removed since the session was last saved:<ul><li>OSM layer name</ul>"
                + "<br>You are about to overwrite the session file \"" + joz.getName()
                + "\". Would you like to proceed?,verticalAlignment=CENTER,verticalTextPosition=CENTER]";

        SessionSaveAction saveAction = SessionSaveAction.getInstance();
        saveAction.setEnabled(true);

        OsmDataLayer osm = SessionWriterTest.createOsmLayer();
        GpxLayer gpx = SessionWriterTest.createGpxLayer();

        JOptionPaneSimpleMocker mocker = new JOptionPaneSimpleMocker(Collections.singletonMap(overrideStr, 0));
        SessionSaveAction.setCurrentSession(jos, false, Arrays.asList(gpx, osm)); //gpx and OSM layer
        MainApplication.getLayerManager().addLayer(gpx); //only gpx layer
        saveAction.actionPerformed(null); //Complain that OSM layer was removed
        assertEquals(1, mocker.getInvocationLog().size());
        assertFalse(jos.exists());
        assertTrue(joz.exists()); //converted jos to joz since the session includes files

        mocker = new JOptionPaneSimpleMocker(Collections.singletonMap(overrideStr, 0));
        joz.delete();
        saveAction.actionPerformed(null); //Do not complain about removed layers
        assertEquals(0, mocker.getInvocationLog().size());
        assertTrue(joz.exists());

        joz.delete();
    }
}
