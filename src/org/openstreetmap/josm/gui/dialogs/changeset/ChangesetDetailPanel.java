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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.actions.downloadtasks.ChangesetHeaderDownloadTask;
import org.openstreetmap.josm.actions.downloadtasks.PostDownloadHandler;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.ChangesetCache;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.history.OpenChangesetPopupMenu;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeListener;
import org.openstreetmap.josm.gui.widgets.JosmTextArea;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.io.NetworkManager;
import org.openstreetmap.josm.io.OnlineResource;
import org.openstreetmap.josm.tools.Destroyable;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.date.DateUtils;

/**
 * This panel displays the properties of the currently selected changeset in the
 * {@link ChangesetCacheManager}.
 * @since 2689
 */
public class ChangesetDetailPanel extends JPanel implements PropertyChangeListener, ChangesetAware, Destroyable {

    // CHECKSTYLE.OFF: SingleSpaceSeparator
    private final JosmTextField tfID        = new JosmTextField(10);
    private final JosmTextArea  taComment   = new JosmTextArea(5, 40);
    private final JosmTextField tfOpen      = new JosmTextField(10);
    private final JosmTextField tfUser      = new JosmTextField("");
    private final JosmTextField tfCreatedOn = new JosmTextField(20);
    private final JosmTextField tfClosedOn  = new JosmTextField(20);

    private final OpenChangesetPopupMenuAction   actOpenChangesetPopupMenu   = new OpenChangesetPopupMenuAction();
    private final DownloadChangesetContentAction actDownloadChangesetContent = new DownloadChangesetContentAction(this);
    private final UpdateChangesetAction          actUpdateChangesets         = new UpdateChangesetAction();
    private final RemoveFromCacheAction          actRemoveFromCache          = new RemoveFromCacheAction();
    private final SelectInCurrentLayerAction     actSelectInCurrentLayer     = new SelectInCurrentLayerAction();
    private final ZoomInCurrentLayerAction       actZoomInCurrentLayerAction = new ZoomInCurrentLayerAction();
    // CHECKSTYLE.ON: SingleSpaceSeparator

    private JButton btnOpenChangesetPopupMenu;

    private transient Changeset currentChangeset;

    protected JPanel buildActionButtonPanel() {
        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JToolBar tb = new JToolBar(JToolBar.VERTICAL);
        tb.setFloatable(false);

        // -- display changeset
        btnOpenChangesetPopupMenu = tb.add(actOpenChangesetPopupMenu);
        actOpenChangesetPopupMenu.initProperties(currentChangeset);

        // -- remove from cache action
        tb.add(actRemoveFromCache);
        actRemoveFromCache.initProperties(currentChangeset);

        // -- changeset update
        tb.add(actUpdateChangesets);
        actUpdateChangesets.initProperties(currentChangeset);

        // -- changeset content download
        tb.add(actDownloadChangesetContent);
        actDownloadChangesetContent.initProperties();

        tb.add(actSelectInCurrentLayer);
        MainApplication.getLayerManager().addActiveLayerChangeListener(actSelectInCurrentLayer);

        tb.add(actZoomInCurrentLayerAction);
        MainApplication.getLayerManager().addActiveLayerChangeListener(actZoomInCurrentLayerAction);

        pnl.add(tb);
        return pnl;
    }

    protected JPanel buildDetailViewPanel() {
        JPanel pnl = new JPanel(new GridBagLayout());

        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        gc.insets = new Insets(0, 0, 2, 3);

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
        setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
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
        taComment.setText(cs.getComment());

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
        DateFormat sdf = DateUtils.getDateTimeFormat(DateFormat.SHORT, DateFormat.SHORT);

        Date createdDate = cs.getCreatedAt();
        Date closedDate = cs.getClosedAt();
        tfCreatedOn.setText(createdDate == null ? "" : sdf.format(createdDate));
        tfClosedOn.setText(closedDate == null ? "" : sdf.format(closedDate));
    }

    /**
     * Constructs a new {@code ChangesetDetailPanel}.
     */
    public ChangesetDetailPanel() {
        build();
    }

    protected void setCurrentChangeset(Changeset cs) {
        currentChangeset = cs;
        if (cs == null) {
            clearView();
        } else {
            updateView(cs);
        }
        actOpenChangesetPopupMenu.initProperties(currentChangeset);
        actDownloadChangesetContent.initProperties();
        actUpdateChangesets.initProperties(currentChangeset);
        actRemoveFromCache.initProperties(currentChangeset);
        actSelectInCurrentLayer.updateEnabledState();
        actZoomInCurrentLayerAction.updateEnabledState();
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

    /**
     * The action for removing the currently selected changeset from the changeset cache
     */
    class RemoveFromCacheAction extends AbstractAction {
        RemoveFromCacheAction() {
            putValue(NAME, tr("Remove from cache"));
            new ImageProvider("dialogs", "delete").getResource().attachImageIcon(this);
            putValue(SHORT_DESCRIPTION, tr("Remove the changeset in the detail view panel from the local cache"));
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            if (currentChangeset == null)
                return;
            ChangesetCache.getInstance().remove(currentChangeset);
        }

        public void initProperties(Changeset cs) {
            setEnabled(cs != null);
        }
    }

    /**
     * Updates the current changeset from the OSM server
     *
     */
    class UpdateChangesetAction extends AbstractAction {
        UpdateChangesetAction() {
            putValue(NAME, tr("Update changeset"));
            new ImageProvider("dialogs/changeset", "updatechangesetcontent").getResource().attachImageIcon(this);
            putValue(SHORT_DESCRIPTION, tr("Update the changeset from the OSM server"));
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            if (currentChangeset == null)
                return;
            ChangesetHeaderDownloadTask task = new ChangesetHeaderDownloadTask(
                    ChangesetDetailPanel.this,
                    Collections.singleton(currentChangeset.getId())
            );
            MainApplication.worker.submit(new PostDownloadHandler(task, task.download()));
        }

        public void initProperties(Changeset cs) {
            setEnabled(cs != null && !NetworkManager.isOffline(OnlineResource.OSM_API));
        }
    }

    /**
     * The action for opening {@link OpenChangesetPopupMenu}
     */
    class OpenChangesetPopupMenuAction extends AbstractAction {
        OpenChangesetPopupMenuAction() {
            putValue(NAME, tr("View changeset"));
            new ImageProvider("help/internet").getResource().attachImageIcon(this);
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            if (currentChangeset != null)
                new OpenChangesetPopupMenu(currentChangeset.getId(), null).show(btnOpenChangesetPopupMenu);
        }

        void initProperties(Changeset cs) {
            setEnabled(cs != null);
        }
    }

    /**
     * Selects the primitives in the content of this changeset in the current data layer.
     *
     */
    class SelectInCurrentLayerAction extends AbstractAction implements ActiveLayerChangeListener {

        SelectInCurrentLayerAction() {
            putValue(NAME, tr("Select in layer"));
            new ImageProvider("dialogs", "select").getResource().attachImageIcon(this);
            putValue(SHORT_DESCRIPTION, tr("Select the primitives in the content of this changeset in the current data layer"));
            updateEnabledState();
        }

        protected void alertNoPrimitivesToSelect() {
            HelpAwareOptionPane.showOptionDialog(
                    ChangesetDetailPanel.this,
                    tr("<html>None of the objects in the content of changeset {0} is available in the current<br>"
                            + "edit layer ''{1}''.</html>",
                            currentChangeset.getId(),
                            Utils.escapeReservedCharactersHTML(MainApplication.getLayerManager().getActiveDataSet().getName())
                    ),
                    tr("Nothing to select"),
                    JOptionPane.WARNING_MESSAGE,
                    HelpUtil.ht("/Dialog/ChangesetCacheManager#NothingToSelectInLayer")
            );
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!isEnabled())
                return;
            DataSet ds = MainApplication.getLayerManager().getActiveDataSet();
            if (ds == null) {
                return;
            }
            Set<OsmPrimitive> target = new HashSet<>();
            for (OsmPrimitive p: ds.allPrimitives()) {
                if (p.isUsable() && p.getChangesetId() == currentChangeset.getId()) {
                    target.add(p);
                }
            }
            if (target.isEmpty()) {
                alertNoPrimitivesToSelect();
                return;
            }
            ds.setSelected(target);
        }

        public void updateEnabledState() {
            setEnabled(MainApplication.getLayerManager().getActiveDataSet() != null && currentChangeset != null);
        }

        @Override
        public void activeOrEditLayerChanged(ActiveLayerChangeEvent e) {
            updateEnabledState();
        }
    }

    /**
     * Zooms to the primitives in the content of this changeset in the current
     * data layer.
     *
     */
    class ZoomInCurrentLayerAction extends AbstractAction implements ActiveLayerChangeListener {

        ZoomInCurrentLayerAction() {
            putValue(NAME, tr("Zoom to in layer"));
            new ImageProvider("dialogs/autoscale", "selection").getResource().attachImageIcon(this);
            putValue(SHORT_DESCRIPTION, tr("Zoom to the objects in the content of this changeset in the current data layer"));
            updateEnabledState();
        }

        protected void alertNoPrimitivesToZoomTo() {
            HelpAwareOptionPane.showOptionDialog(
                    ChangesetDetailPanel.this,
                    tr("<html>None of the objects in the content of changeset {0} is available in the current<br>"
                            + "edit layer ''{1}''.</html>",
                            currentChangeset.getId(),
                            MainApplication.getLayerManager().getActiveDataSet().getName()
                    ),
                    tr("Nothing to zoom to"),
                    JOptionPane.WARNING_MESSAGE,
                    HelpUtil.ht("/Dialog/ChangesetCacheManager#NothingToZoomTo")
            );
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!isEnabled())
                return;
            DataSet ds = MainApplication.getLayerManager().getActiveDataSet();
            if (ds == null) {
                return;
            }
            Set<OsmPrimitive> target = new HashSet<>();
            for (OsmPrimitive p: ds.allPrimitives()) {
                if (p.isUsable() && p.getChangesetId() == currentChangeset.getId()) {
                    target.add(p);
                }
            }
            if (target.isEmpty()) {
                alertNoPrimitivesToZoomTo();
                return;
            }
            ds.setSelected(target);
            AutoScaleAction.zoomToSelection();
        }

        public void updateEnabledState() {
            setEnabled(MainApplication.getLayerManager().getActiveDataSet() != null && currentChangeset != null);
        }

        @Override
        public void activeOrEditLayerChanged(ActiveLayerChangeEvent e) {
            updateEnabledState();
        }
    }

    @Override
    public Changeset getCurrentChangeset() {
        return currentChangeset;
    }

    @Override
    public void destroy() {
        MainApplication.getLayerManager().removeActiveLayerChangeListener(actSelectInCurrentLayer);
        MainApplication.getLayerManager().removeActiveLayerChangeListener(actZoomInCurrentLayerAction);
    }
}
