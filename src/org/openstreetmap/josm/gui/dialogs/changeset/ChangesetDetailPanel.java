// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.ChangesetCache;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapView.EditLayerChangeListener;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.widgets.JosmTextArea;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * This panel displays the properties of the currently selected changeset in the
 * {@link ChangesetCacheManager}.
 *
 */
public class ChangesetDetailPanel extends JPanel implements PropertyChangeListener {

    private final JosmTextField tfID        = new JosmTextField(10);
    private final JosmTextArea  taComment   = new JosmTextArea(5,40);
    private final JosmTextField tfOpen      = new JosmTextField(10);
    private final JosmTextField tfUser      = new JosmTextField("");
    private final JosmTextField tfCreatedOn = new JosmTextField(20);
    private final JosmTextField tfClosedOn  = new JosmTextField(20);

    private final DownloadChangesetContentAction actDownloadChangesetContent = new DownloadChangesetContentAction();
    private final UpdateChangesetAction          actUpdateChangesets         = new UpdateChangesetAction();
    private final RemoveFromCacheAction          actRemoveFromCache          = new RemoveFromCacheAction();
    private final SelectInCurrentLayerAction     actSelectInCurrentLayer     = new SelectInCurrentLayerAction();
    private final ZoomInCurrentLayerAction       actZoomInCurrentLayerAction = new ZoomInCurrentLayerAction();

    private Changeset current = null;

    protected JPanel buildActionButtonPanel() {
        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JToolBar tb = new JToolBar(JToolBar.VERTICAL);
        tb.setFloatable(false);

        // -- remove from cache action
        tb.add(actRemoveFromCache);
        actRemoveFromCache.initProperties(current);

        // -- changeset update
        tb.add(actUpdateChangesets);
        actUpdateChangesets.initProperties(current);

        // -- changeset content download
        tb.add(actDownloadChangesetContent);
        actDownloadChangesetContent.initProperties(current);

        tb.add(actSelectInCurrentLayer);
        MapView.addEditLayerChangeListener(actSelectInCurrentLayer);

        tb.add(actZoomInCurrentLayerAction);
        MapView.addEditLayerChangeListener(actZoomInCurrentLayerAction);

        addComponentListener(
                new ComponentAdapter() {
                    @Override
                    public void componentHidden(ComponentEvent e) {
                        // make sure the listener is unregistered when the panel becomes
                        // invisible
                        MapView.removeEditLayerChangeListener(actSelectInCurrentLayer);
                        MapView.removeEditLayerChangeListener(actZoomInCurrentLayerAction);
                    }
                }
        );

        pnl.add(tb);
        return pnl;
    }

    protected JPanel buildDetailViewPanel() {
        JPanel pnl = new JPanel(new GridBagLayout());

        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        gc.insets = new Insets(0,0,2,3);

        //-- id
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0.0;
        pnl.add(new JLabel(tr("ID:")), gc);

        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0.0;
        gc.gridx = 1;
        pnl.add(tfID, gc);
        tfID.setEditable(false);

        //-- comment
        gc.gridx = 0;
        gc.gridy = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0.0;
        pnl.add(new JLabel(tr("Comment:")), gc);

        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        gc.gridx = 1;
        pnl.add(taComment, gc);
        taComment.setEditable(false);

        //-- Open/Closed
        gc.gridx = 0;
        gc.gridy = 2;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        pnl.add(new JLabel(tr("Open/Closed:")), gc);

        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridx = 1;
        pnl.add(tfOpen, gc);
        tfOpen.setEditable(false);

        //-- Created by:
        gc.gridx = 0;
        gc.gridy = 3;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0.0;
        pnl.add(new JLabel(tr("Created by:")), gc);

        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.gridx = 1;
        pnl.add(tfUser, gc);
        tfUser.setEditable(false);

        //-- Created On:
        gc.gridx = 0;
        gc.gridy = 4;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0.0;
        pnl.add(new JLabel(tr("Created on:")), gc);

        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridx = 1;
        pnl.add(tfCreatedOn, gc);
        tfCreatedOn.setEditable(false);

        //-- Closed On:
        gc.gridx = 0;
        gc.gridy = 5;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0.0;
        pnl.add(new JLabel(tr("Closed on:")), gc);

        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridx = 1;
        pnl.add(tfClosedOn, gc);
        tfClosedOn.setEditable(false);

        return pnl;
    }

    protected final void build() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
        add(buildDetailViewPanel(), BorderLayout.CENTER);
        add(buildActionButtonPanel(), BorderLayout.WEST);
    }

    protected void clearView() {
        tfID.setText("");
        taComment.setText("");
        tfOpen.setText("");
        tfUser.setText("");
        tfCreatedOn.setText("");
        tfClosedOn.setText("");
    }

    protected void updateView(Changeset cs) {
        String msg;
        if (cs == null) return;
        tfID.setText(Integer.toString(cs.getId()));
        String comment = cs.get("comment");
        taComment.setText(comment == null ? "" : comment);

        if (cs.isOpen()) {
            msg = trc("changeset.state", "Open");
        } else {
            msg = trc("changeset.state", "Closed");
        }
        tfOpen.setText(msg);

        if (cs.getUser() == null) {
            msg = tr("anonymous");
        } else {
            msg = cs.getUser().getName();
        }
        tfUser.setText(msg);
        DateFormat sdf = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

        tfCreatedOn.setText(cs.getCreatedAt() == null ? "" : sdf.format(cs.getCreatedAt()));
        tfClosedOn.setText(cs.getClosedAt() == null ? "" : sdf.format(cs.getClosedAt()));
    }

    /**
     * Constructs a new {@code ChangesetDetailPanel}.
     */
    public ChangesetDetailPanel() {
        build();
    }

    protected void setCurrentChangeset(Changeset cs) {
        current = cs;
        if (cs == null) {
            clearView();
        } else {
            updateView(cs);
        }
        actDownloadChangesetContent.initProperties(current);
        actUpdateChangesets.initProperties(current);
        actRemoveFromCache.initProperties(current);
        actSelectInCurrentLayer.updateEnabledState();
        actZoomInCurrentLayerAction.updateEnabledState();
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

    /**
     * The action for removing the currently selected changeset from the changeset cache
     */
    class RemoveFromCacheAction extends AbstractAction {
        public RemoveFromCacheAction() {
            putValue(NAME, tr("Remove from cache"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "delete"));
            putValue(SHORT_DESCRIPTION, tr("Remove the changeset in the detail view panel from the local cache"));
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            if (current == null)
                return;
            ChangesetCache.getInstance().remove(current);
        }

        public void initProperties(Changeset cs) {
            setEnabled(cs != null);
        }
    }

    /**
     * Removes the selected changesets from the local changeset cache
     *
     */
    class DownloadChangesetContentAction extends AbstractAction {
        public DownloadChangesetContentAction() {
            putValue(NAME, tr("Download content"));
            putValue(SMALL_ICON, ChangesetCacheManager.DOWNLOAD_CONTENT_ICON);
            putValue(SHORT_DESCRIPTION, tr("Download the changeset content from the OSM server"));
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            if (current == null) return;
            ChangesetContentDownloadTask task = new ChangesetContentDownloadTask(ChangesetDetailPanel.this, current.getId());
            ChangesetCacheManager.getInstance().runDownloadTask(task);
        }

        public void initProperties(Changeset cs) {
            if (cs == null) {
                setEnabled(false);
                return;
            } else {
                setEnabled(true);
            }
            if (cs.getContent() == null) {
                putValue(NAME, tr("Download content"));
                putValue(SMALL_ICON, ChangesetCacheManager.DOWNLOAD_CONTENT_ICON);
                putValue(SHORT_DESCRIPTION, tr("Download the changeset content from the OSM server"));
            } else {
                putValue(NAME, tr("Update content"));
                putValue(SMALL_ICON, ChangesetCacheManager.UPDATE_CONTENT_ICON);
                putValue(SHORT_DESCRIPTION, tr("Update the changeset content from the OSM server"));
            }
        }
    }

    /**
     * Updates the current changeset from the OSM server
     *
     */
    class UpdateChangesetAction extends AbstractAction{
        public UpdateChangesetAction() {
            putValue(NAME, tr("Update changeset"));
            putValue(SMALL_ICON, ChangesetCacheManager.UPDATE_CONTENT_ICON);
            putValue(SHORT_DESCRIPTION, tr("Update the changeset from the OSM server"));
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            if (current == null) return;
            Main.worker.submit(
                    new ChangesetHeaderDownloadTask(
                            ChangesetDetailPanel.this,
                            Collections.singleton(current.getId())
                    )
            );
        }

        public void initProperties(Changeset cs) {
            if (cs == null) {
                setEnabled(false);
                return;
            } else {
                setEnabled(true);
            }
        }
    }

    /**
     * Selects the primitives in the content of this changeset in the current
     * data layer.
     *
     */
    class SelectInCurrentLayerAction extends AbstractAction implements EditLayerChangeListener{

        public SelectInCurrentLayerAction() {
            putValue(NAME, tr("Select in layer"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "select"));
            putValue(SHORT_DESCRIPTION, tr("Select the primitives in the content of this changeset in the current data layer"));
            updateEnabledState();
        }

        protected void alertNoPrimitivesToSelect(Collection<OsmPrimitive> primitives) {
            HelpAwareOptionPane.showOptionDialog(
                    ChangesetDetailPanel.this,
                    tr("<html>None of the objects in the content of changeset {0} is available in the current<br>"
                            + "edit layer ''{1}''.</html>",
                            current.getId(),
                            Main.main.getEditLayer().getName()
                    ),
                    tr("Nothing to select"),
                    JOptionPane.WARNING_MESSAGE,
                    HelpUtil.ht("/Dialog/ChangesetCacheManager#NothingToSelectInLayer")
            );
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (!isEnabled())
                return;
            if (Main.main == null || !Main.main.hasEditLayer()) return;
            OsmDataLayer layer = Main.main.getEditLayer();
            Set<OsmPrimitive> target = new HashSet<OsmPrimitive>();
            for (OsmPrimitive p: layer.data.allPrimitives()) {
                if (p.isUsable() && p.getChangesetId() == current.getId()) {
                    target.add(p);
                }
            }
            if (target.isEmpty()) {
                alertNoPrimitivesToSelect(target);
                return;
            }
            layer.data.setSelected(target);
        }

        public void updateEnabledState() {
            if (Main.main == null || !Main.main.hasEditLayer()) {
                setEnabled(false);
                return;
            }
            setEnabled(current != null);
        }

        @Override
        public void editLayerChanged(OsmDataLayer oldLayer, OsmDataLayer newLayer) {
            updateEnabledState();
        }
    }

    /**
     * Zooms to the primitives in the content of this changeset in the current
     * data layer.
     *
     */
    class ZoomInCurrentLayerAction extends AbstractAction implements EditLayerChangeListener{

        public ZoomInCurrentLayerAction() {
            putValue(NAME, tr("Zoom to in layer"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs/autoscale", "selection"));
            putValue(SHORT_DESCRIPTION, tr("Zoom to the objects in the content of this changeset in the current data layer"));
            updateEnabledState();
        }

        protected void alertNoPrimitivesToZoomTo() {
            HelpAwareOptionPane.showOptionDialog(
                    ChangesetDetailPanel.this,
                    tr("<html>None of the objects in the content of changeset {0} is available in the current<br>"
                            + "edit layer ''{1}''.</html>",
                            current.getId(),
                            Main.main.getEditLayer().getName()
                    ),
                    tr("Nothing to zoom to"),
                    JOptionPane.WARNING_MESSAGE,
                    HelpUtil.ht("/Dialog/ChangesetCacheManager#NothingToZoomTo")
            );
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (!isEnabled())
                return;
            if (Main.main == null || !Main.main.hasEditLayer()) return;
            OsmDataLayer layer = Main.main.getEditLayer();
            Set<OsmPrimitive> target = new HashSet<OsmPrimitive>();
            for (OsmPrimitive p: layer.data.allPrimitives()) {
                if (p.isUsable() && p.getChangesetId() == current.getId()) {
                    target.add(p);
                }
            }
            if (target.isEmpty()) {
                alertNoPrimitivesToZoomTo();
                return;
            }
            layer.data.setSelected(target);
            AutoScaleAction.zoomToSelection();
        }

        public void updateEnabledState() {
            if (Main.main == null || !Main.main.hasEditLayer()) {
                setEnabled(false);
                return;
            }
            setEnabled(current != null);
        }

        @Override
        public void editLayerChanged(OsmDataLayer oldLayer, OsmDataLayer newLayer) {
            updateEnabledState();
        }
    }
}
