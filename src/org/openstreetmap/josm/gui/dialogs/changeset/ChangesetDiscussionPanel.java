// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.io.OnlineResource;

/**
 * The panel which displays the public discussion around a changeset in a scrollable table.
 *
 * It listens to property change events for {@link ChangesetCacheManagerModel#CHANGESET_IN_DETAIL_VIEW_PROP}
 * and updates its view accordingly.
 *
 * @since 7704
 */
public class ChangesetDiscussionPanel extends JPanel implements PropertyChangeListener {

    private final UpdateChangesetDiscussionAction actUpdateChangesets = new UpdateChangesetDiscussionAction();

    private Changeset current = null;

    protected JPanel buildActionButtonPanel() {
        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JToolBar tb = new JToolBar(JToolBar.VERTICAL);
        tb.setFloatable(false);

        // -- changeset discussion update
        tb.add(actUpdateChangesets);
        actUpdateChangesets.initProperties(current);

        pnl.add(tb);
        return pnl;
    }

    /**
     * Updates the current changeset discussion from the OSM server
     */
    class UpdateChangesetDiscussionAction extends AbstractAction {
        public UpdateChangesetDiscussionAction() {
            putValue(NAME, tr("Update changeset discussion"));
            putValue(SMALL_ICON, ChangesetCacheManager.UPDATE_CONTENT_ICON);
            putValue(SHORT_DESCRIPTION, tr("Update the changeset discussion from the OSM server"));
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            if (current == null) return;
            Main.worker.submit(
                    new ChangesetHeaderDownloadTask(
                            ChangesetDiscussionPanel.this,
                            Collections.singleton(current.getId()),
                            true /* include discussion */
                    )
            );
        }

        public void initProperties(Changeset cs) {
            setEnabled(cs != null && !Main.isOffline(OnlineResource.OSM_API));
        }
    }

    /**
     * Constructs a new {@code ChangesetDiscussionPanel}.
     */
    public ChangesetDiscussionPanel() {
        build();
    }

    protected void setCurrentChangeset(Changeset cs) {
        current = cs;
        if (cs == null) {
            clearView();
        } else {
            updateView(cs);
        }
        actUpdateChangesets.initProperties(current);
    }

    protected final void build() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
        //add(buildDetailViewPanel(), BorderLayout.CENTER);
        add(buildActionButtonPanel(), BorderLayout.WEST);
    }

    protected void clearView() {
        // TODO
    }

    protected void updateView(Changeset cs) {
        // TODO
    }

    /* ---------------------------------------------------------------------------- */
    /* interface PropertyChangeListener                                             */
    /* ---------------------------------------------------------------------------- */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (! evt.getPropertyName().equals(ChangesetCacheManagerModel.CHANGESET_IN_DETAIL_VIEW_PROP))
            return;
        setCurrentChangeset((Changeset)evt.getNewValue());
    }
}
