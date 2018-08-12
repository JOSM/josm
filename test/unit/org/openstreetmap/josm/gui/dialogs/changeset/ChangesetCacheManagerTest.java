// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import static org.junit.Assert.assertNotNull;

import java.util.Collections;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.gui.dialogs.changeset.ChangesetCacheManager.CancelAction;
import org.openstreetmap.josm.gui.dialogs.changeset.ChangesetCacheManager.ChangesetDetailViewSynchronizer;
import org.openstreetmap.josm.gui.dialogs.changeset.ChangesetCacheManager.CloseSelectedChangesetsAction;
import org.openstreetmap.josm.gui.dialogs.changeset.ChangesetCacheManager.DownloadMyChangesets;
import org.openstreetmap.josm.gui.dialogs.changeset.ChangesetCacheManager.DownloadSelectedChangesetContentAction;
import org.openstreetmap.josm.gui.dialogs.changeset.ChangesetCacheManager.DownloadSelectedChangesetsAction;
import org.openstreetmap.josm.gui.dialogs.changeset.ChangesetCacheManager.QueryAction;
import org.openstreetmap.josm.gui.dialogs.changeset.ChangesetCacheManager.RemoveFromCacheAction;
import org.openstreetmap.josm.gui.dialogs.changeset.ChangesetCacheManager.ShowDetailAction;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link ChangesetCacheManager} class.
 */
public class ChangesetCacheManagerTest {

    /**
     * Setup tests
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Unit test of {@link ChangesetCacheManager#destroyInstance}.
     */
    @Test
    public void testDestroyInstance() {
        ChangesetCacheManager.destroyInstance();
    }

    /**
     * Unit test of {@link ChangesetCacheManager#buildButtonPanel},
     *              {@link ChangesetCacheManager#buildToolbarPanel}.
     *              {@link ChangesetCacheManager#buildModel}.
     */
    @Test
    public void testBuild() {
        assertNotNull(ChangesetCacheManager.buildButtonPanel());
        assertNotNull(ChangesetCacheManager.buildToolbarPanel());
        assertNotNull(ChangesetCacheManager.buildModel());
    }

    /**
     * Unit test of {@link ChangesetCacheManager.ChangesetDetailViewSynchronizer} class.
     */
    @Test
    public void testChangesetDetailViewSynchronizer() {
        new ChangesetDetailViewSynchronizer(new ChangesetCacheManagerModel(null) {
            @Override
            public List<Changeset> getSelectedChangesets() {
                return Collections.emptyList();
            }
        }).valueChanged(null);

        new ChangesetDetailViewSynchronizer(new ChangesetCacheManagerModel(null) {
            @Override
            public List<Changeset> getSelectedChangesets() {
                return Collections.singletonList(new Changeset());
            }
        }).valueChanged(null);
    }

    /**
     * Unit test of {@link ChangesetCacheManager.CancelAction} class.
     */
    @Test
    public void testCancelAction() {
        new CancelAction().actionPerformed(null);
    }

    /**
     * Unit test of {@link ChangesetCacheManager.CloseSelectedChangesetsAction} class.
     */
    @Test
    public void testCloseSelectedChangesetsAction() {
        CloseSelectedChangesetsAction action = new CloseSelectedChangesetsAction(new ChangesetCacheManagerModel(null) {
            @Override
            public List<Changeset> getSelectedChangesets() {
                return Collections.singletonList(new Changeset());
            }
        });
        action.valueChanged(null);
        action.actionPerformed(null);
    }

    /**
     * Unit test of {@link ChangesetCacheManager.DownloadMyChangesets} class.
     */
    @Test
    public void testDownloadMyChangesets() {
        new DownloadMyChangesets().actionPerformed(null);
    }

    /**
     * Unit test of {@link ChangesetCacheManager.DownloadSelectedChangesetContentAction} class.
     */
    @Test
    public void testDownloadSelectedChangesetContentAction() {
        DownloadSelectedChangesetContentAction action = new DownloadSelectedChangesetContentAction(ChangesetCacheManager.buildModel());
        action.valueChanged(null);
        action.actionPerformed(null);
    }

    /**
     * Unit test of {@link ChangesetCacheManager.DownloadSelectedChangesetsAction} class.
     */
    @Test
    public void testDownloadSelectedChangesetsAction() {
        DownloadSelectedChangesetsAction action = new DownloadSelectedChangesetsAction(ChangesetCacheManager.buildModel());
        action.valueChanged(null);
        action.actionPerformed(null);
    }

    /**
     * Unit test of {@link ChangesetCacheManager.QueryAction} class.
     */
    @Test
    public void testQueryAction() {
        new QueryAction().actionPerformed(null);
    }

    /**
     * Unit test of {@link ChangesetCacheManager.RemoveFromCacheAction} class.
     */
    @Test
    public void testRemoveFromCacheAction() {
        RemoveFromCacheAction action = new RemoveFromCacheAction(ChangesetCacheManager.buildModel());
        action.valueChanged(null);
        action.actionPerformed(null);
    }

    /**
     * Unit test of {@link ChangesetCacheManager.ShowDetailAction} class.
     */
    @Test
    public void testShowDetailAction() {
        new ShowDetailAction(ChangesetCacheManager.buildModel()).actionPerformed(null);
    }
}
