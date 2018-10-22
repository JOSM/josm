// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.testutils.mockers.JOptionPaneSimpleMocker;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link SaveLayersDialog} class.
 */
public class SaveLayersDialogTest {

    /**
     * Setup tests
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Test of {@link SaveLayersDialog#confirmSaveLayerInfosOK}.
     */
    @Test
    public void testConfirmSaveLayerInfosOK() {
        final List<SaveLayerInfo> list = Collections.singletonList(new SaveLayerInfo(new OsmDataLayer(new DataSet(), null, null)));

        final JOptionPaneSimpleMocker jopsMocker = new JOptionPaneSimpleMocker() {
            @Override
            protected void act(final Object message) {
                // use this opportunity to assert that our SaveLayerInfo is the single option in the JList
                @SuppressWarnings("unchecked")
                final JList<SaveLayerInfo> jList = (JList<SaveLayerInfo>) ((JComponent) message).getComponent(1);
                assertEquals(1, jList.getModel().getSize());
                assertEquals(list.get(0), jList.getModel().getElementAt(0));
            }

            @Override
            protected String getStringFromMessage(final Object message) {
                return ((JLabel) ((JComponent) message).getComponent(0)).getText();
            }
        };

        jopsMocker.getMockResultMap().put(
            "<html>1 layer has unresolved conflicts.<br>Either resolve them first or discard the "
            + "modifications.<br>Layer with conflicts:</html>", JOptionPane.OK_OPTION
        );

        assertFalse(SaveLayersDialog.confirmSaveLayerInfosOK(new SaveLayersModel() {
            @Override
            public List<SaveLayerInfo> getLayersWithConflictsAndUploadRequest() {
                return list;
            }
        }));

        assertEquals(1, jopsMocker.getInvocationLog().size());
        Object[] invocationLogEntry = jopsMocker.getInvocationLog().get(0);
        assertEquals(JOptionPane.OK_OPTION, (int) invocationLogEntry[0]);
        assertEquals("Unsaved data and conflicts", invocationLogEntry[2]);

        jopsMocker.resetInvocationLog();
        jopsMocker.getMockResultMap().clear();
        jopsMocker.getMockResultMap().put(
            "<html>1 layer needs saving but has no associated file.<br>Either select a file for this "
            + "layer or discard the changes.<br>Layer without a file:</html>", JOptionPane.OK_OPTION
        );

        assertFalse(SaveLayersDialog.confirmSaveLayerInfosOK(new SaveLayersModel() {
            @Override
            public List<SaveLayerInfo> getLayersWithoutFilesAndSaveRequest() {
                return list;
            }
        }));

        assertEquals(1, jopsMocker.getInvocationLog().size());
        invocationLogEntry = jopsMocker.getInvocationLog().get(0);
        assertEquals(JOptionPane.OK_OPTION, (int) invocationLogEntry[0]);
        assertEquals("Unsaved data and missing associated file", invocationLogEntry[2]);

        jopsMocker.resetInvocationLog();
        jopsMocker.getMockResultMap().clear();
        jopsMocker.getMockResultMap().put(
            "<html>1 layer needs saving but has an associated file<br>which cannot be written.<br>Either "
            + "select another file for this layer or discard the changes.<br>Layer with a non-writable "
            + "file:</html>", JOptionPane.OK_OPTION
        );

        assertFalse(SaveLayersDialog.confirmSaveLayerInfosOK(new SaveLayersModel() {
            @Override
            public List<SaveLayerInfo> getLayersWithIllegalFilesAndSaveRequest() {
                return list;
            }
        }));

        assertEquals(1, jopsMocker.getInvocationLog().size());
        invocationLogEntry = jopsMocker.getInvocationLog().get(0);
        assertEquals(JOptionPane.OK_OPTION, (int) invocationLogEntry[0]);
        assertEquals("Unsaved data non-writable files", invocationLogEntry[2]);

        jopsMocker.resetInvocationLog();
        jopsMocker.getMockResultMap().clear();

        assertTrue(SaveLayersDialog.confirmSaveLayerInfosOK(new SaveLayersModel()));
    }
}
