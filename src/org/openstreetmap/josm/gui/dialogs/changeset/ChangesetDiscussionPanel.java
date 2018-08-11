// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;

import org.openstreetmap.josm.actions.downloadtasks.ChangesetHeaderDownloadTask;
import org.openstreetmap.josm.actions.downloadtasks.PostDownloadHandler;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.io.NetworkManager;
import org.openstreetmap.josm.io.OnlineResource;
import org.openstreetmap.josm.tools.ImageProvider;

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

    private final ChangesetDiscussionTableModel model = new ChangesetDiscussionTableModel();

    private JTable table;

    private transient Changeset current;

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
        UpdateChangesetDiscussionAction() {
            putValue(NAME, tr("Update changeset discussion"));
            new ImageProvider("dialogs/changeset", "updatechangesetcontent").getResource().attachImageIcon(this);
            putValue(SHORT_DESCRIPTION, tr("Update the changeset discussion from the OSM server"));
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            if (current == null)
                return;
            ChangesetHeaderDownloadTask task = new ChangesetHeaderDownloadTask(
                    ChangesetDiscussionPanel.this,
                    Collections.singleton(current.getId()),
                    true /* include discussion */
            );
            MainApplication.worker.submit(new PostDownloadHandler(task, task.download()));
        }

        public void initProperties(Changeset cs) {
            setEnabled(cs != null && !NetworkManager.isOffline(OnlineResource.OSM_API));
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
        if (cs != null && cs.getDiscussion().size() < cs.getCommentsCount()) {
            actUpdateChangesets.actionPerformed(null);
        }
    }

    protected final void build() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        add(buildActionButtonPanel(), BorderLayout.WEST);
        add(buildDiscussionPanel(), BorderLayout.CENTER);
    }

    private Component buildDiscussionPanel() {
        JPanel pnl = new JPanel(new BorderLayout());
        table = new JTable(model, new ChangesetDiscussionTableColumnModel());
        table.getColumnModel().getColumn(2).addPropertyChangeListener(evt -> {
            if ("width".equals(evt.getPropertyName())) {
                updateRowHeights();
            }
        });
        pnl.add(new JScrollPane(table), BorderLayout.CENTER);
        return pnl;
    }

    protected void clearView() {
        model.populate(null);
    }

    protected void updateView(Changeset cs) {
        model.populate(cs.getDiscussion());
        updateRowHeights();
    }

    protected void updateRowHeights() {
        int intercellWidth = table.getIntercellSpacing().width;
        int colWidth = table.getColumnModel().getColumn(2).getWidth();
        // Update row heights
        for (int row = 0; row < table.getRowCount(); row++) {
            int rowHeight = table.getRowHeight();

            Component comp = table.prepareRenderer(table.getCellRenderer(row, 2), row, 2);
            // constrain width of component
            comp.setBounds(new Rectangle(0, 0, colWidth - intercellWidth, Integer.MAX_VALUE));
            rowHeight = Math.max(rowHeight, comp.getPreferredSize().height);

            table.setRowHeight(row, rowHeight);
        }
    }

    /* ---------------------------------------------------------------------------- */
    /* interface PropertyChangeListener                                             */
    /* ---------------------------------------------------------------------------- */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (!evt.getPropertyName().equals(ChangesetCacheManagerModel.CHANGESET_IN_DETAIL_VIEW_PROP))
            return;
        setCurrentChangeset((Changeset) evt.getNewValue());
    }
}
