// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.upload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import mockit.Mock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.APIDataSet;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.DataSource;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.preferences.sources.ValidatorPrefHelper;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.testutils.mockers.ExtendedDialogMocker;

/**
 * Unit tests for class {@link ValidateUploadHook}.
 */
class ValidateUploadHookTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().main().projection().fakeAPI().timeout(30000);

    /**
     * Test of {@link ValidateUploadHook#checkUpload} method.
     */
    @Test
    void testCheckUpload() {
        assertTrue(new ValidateUploadHook().checkUpload(new APIDataSet()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testUploadOtherErrors(boolean otherEnabled) {
        ValidatorPrefHelper.PREF_OTHER_UPLOAD.put(otherEnabled);
        final DataSet ds = new DataSet();
        final Way building = TestUtils.newWay("building=yes", new Node(new LatLon(33.2287665, -111.8259225)),
                new Node(new LatLon(33.2287335, -111.8257513)), new Node(new LatLon(33.2285316, -111.8258086)),
                new Node(new LatLon(33.2285696, -111.8259781)));
        ds.addPrimitiveRecursive(building);
        building.addNode(building.firstNode());
        ds.addDataSource(new DataSource(new Bounds(33, -112, 34, -111), ""));
        MainApplication.getLayerManager().addLayer(new OsmDataLayer(ds,
                "ValidateUploadHookTest#testUploadOtherErrors", null));
        final ExtendedDialogMocker mocker =
                new ExtendedDialogMocker(Collections.singletonMap("Suspicious data found. Upload anyway?", "Cancel")) {
                    @Override
                    protected String getString(ExtendedDialog instance) {
                        return instance.getTitle();
                    }

                    @Mock
                    public void dispose() {
                        // Do nothing
                    }
                };
        new ValidateUploadHook().checkUpload(new APIDataSet(ds));
        assertEquals(!otherEnabled, mocker.getInvocationLog().isEmpty());
    }
}
