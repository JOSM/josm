// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.awt.GraphicsEnvironment;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.command.AddPrimitivesCommand;
import org.openstreetmap.josm.data.UserIdentityManager;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.io.UploadDialog;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.LayerEnvironment;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.OsmApiType;
import org.openstreetmap.josm.testutils.annotations.Projection;
import org.openstreetmap.josm.testutils.annotations.Territories;
import org.openstreetmap.josm.testutils.annotations.Users;
import org.openstreetmap.josm.testutils.mockers.WindowMocker;
import org.openstreetmap.josm.tools.Logging;

import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;

/**
 * Test class for {@link UploadAction}
 * @author Taylor Smock
 */
@BasicPreferences
@LayerEnvironment
@Main
@OsmApiType(OsmApiType.APIType.FAKE)
@Projection
@Territories(Territories.Initialize.ALL)
@Users
class UploadActionTest {
    /**
     * Non-regression test for JOSM #21476.
     */
    @Test
    void testNonRegression21476() {
        TestUtils.assumeWorkingJMockit();
        Logging.clearLastErrorAndWarnings();
        new WindowMocker();
        new UploadDialogMock();
        // Set up the preconditions. This test acts more like an integration test, in so far as it also tests
        // unrelated code.
        UserIdentityManager.getInstance().setAnonymous();
        final DataSet testData = new DataSet();
        MainApplication.getLayerManager().addLayer(new OsmDataLayer(testData, "testNonRegression21476", null));
        final Node toAdd = new Node(new LatLon(20, 20));
        toAdd.put("shop", "butcher");
        final AddPrimitivesCommand command = new AddPrimitivesCommand(Collections.singletonList(toAdd.save()), testData);
        command.executeCommand();
        final UploadAction uploadAction = new UploadAction();
        uploadAction.updateEnabledState();
        assertTrue(uploadAction.isEnabled());
        // Perform the actual "test" -- we don't want it to throw an exception
        assertDoesNotThrow(() -> uploadAction.actionPerformed(null));
        // Sync threads
        GuiHelper.runInEDT(() -> {
            // sync edt
        });
        try {
            MainApplication.worker.submit(() -> {
                // sync worker
            }).get(1, TimeUnit.SECONDS);
            assertTrue(Logging.getLastErrorAndWarnings().isEmpty());
        } catch (Exception exception) {
            fail(exception);
        } finally {
            Logging.clearLastErrorAndWarnings();
        }
    }

    private static class UploadDialogMock extends MockUp<UploadDialog> {
        @Mock
        public void pack(final Invocation invocation) {
            if (!GraphicsEnvironment.isHeadless()) {
                invocation.proceed();
            }
        }

        @Mock
        public void setVisible(final Invocation invocation, final boolean visible) {
            if (!GraphicsEnvironment.isHeadless()) {
                invocation.proceed(visible);
            }
        }

        @Mock
        public final boolean isCanceled(final Invocation invocation) {
            if (!GraphicsEnvironment.isHeadless()) {
                return invocation.proceed();
            }
            return true;
        }
    }
}
