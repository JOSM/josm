// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.UploadPolicy;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.mockers.JOptionPaneSimpleMocker;
import org.openstreetmap.josm.testutils.mockers.WindowMocker;

import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;

/**
 * Unit tests of {@link SaveLayersDialog} class.
 */
@BasicPreferences
class SaveLayersDialogTest {
    /**
     * Test of {@link SaveLayersDialog#confirmSaveLayerInfosOK}.
     */
    @Test
    void testConfirmSaveLayerInfosOK() {
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

    /**
     * Non-regression test for #22817: No warning when deleting a layer with changes and discourages upload
     * @param policy The upload policy to test
     * @throws IOException if an error occurs
     * @throws IllegalDataException if an error occurs
     */
    @ParameterizedTest
    @EnumSource(value = UploadPolicy.class)
    void testNonRegression22817(UploadPolicy policy) throws IOException, IllegalDataException {
        File file = new File(TestUtils.getRegressionDataFile(22817, "data.osm"));
        InputStream is = new FileInputStream(file);
        final OsmDataLayer osmDataLayer = new OsmDataLayer(OsmReader.parseDataSet(is, null), null, null);
        osmDataLayer.onPostLoadFromFile();
        osmDataLayer.getDataSet().setUploadPolicy(policy);
        osmDataLayer.setAssociatedFile(file);
        assertTrue(osmDataLayer.getDataSet().isModified());
        assertFalse(osmDataLayer.requiresSaveToFile());
        assertTrue(osmDataLayer.getDataSet().requiresUploadToServer());
        assertEquals(policy != UploadPolicy.BLOCKED, osmDataLayer.requiresUploadToServer());
        assertEquals(policy != UploadPolicy.BLOCKED, osmDataLayer.isUploadable());
        new WindowMocker();
        // Needed since the *first call* is to check whether we are in a headless environment
        new GraphicsEnvironmentMock();
        // Needed since we need to mock out the UI
        SaveLayersDialogMock saveLayersDialogMock = new SaveLayersDialogMock();
        assertTrue(SaveLayersDialog.saveUnsavedModifications(Collections.singleton(osmDataLayer), SaveLayersDialog.Reason.DELETE));
        int res = saveLayersDialogMock.getUserActionCalled;
        if (policy == UploadPolicy.NORMAL) {
            assertEquals(1, res, "The user should have been asked for an action on the layer");
        } else {
            assertEquals(0, res, "The user should not have been asked for an action on the layer");

        }
    }

    private static class GraphicsEnvironmentMock extends MockUp<GraphicsEnvironment> {
        @Mock
        public static boolean isHeadless(Invocation invocation) {
            return false;
        }
    }

    private static class SaveLayersDialogMock extends MockUp<SaveLayersDialog> {
        private final SaveLayersModel model = new SaveLayersModel();
        private int getUserActionCalled = 0;
        @Mock
        public void $init(Component parent) {
            // Do nothing
        }

        @Mock
        public void prepareForSavingAndUpdatingLayers(final SaveLayersDialog.Reason reason) {
            // Do nothing
        }

        @Mock
        public SaveLayersModel getModel() {
            return this.model;
        }

        @Mock
        public void setVisible(boolean b) {
            // Do nothing
        }

        @Mock
        public SaveLayersDialog.UserAction getUserAction() {
            this.getUserActionCalled++;
            return SaveLayersDialog.UserAction.PROCEED;
        }

        @Mock
        public void closeDialog() {
            // Do nothing
        }
    }
}
