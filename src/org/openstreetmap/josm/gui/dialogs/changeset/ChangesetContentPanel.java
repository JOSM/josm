// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.history.History;
import org.openstreetmap.josm.data.osm.history.HistoryDataSet;
import org.openstreetmap.josm.data.osm.history.HistoryOsmPrimitive;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapView.EditLayerChangeListener;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.history.HistoryBrowserDialogManager;
import org.openstreetmap.josm.gui.history.HistoryLoadTask;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.gui.widgets.PopupMenuLauncher;
import org.openstreetmap.josm.tools.BugReportExceptionHandler;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * The panel which displays the content of a changeset in a scollable table.
 *
 * It listens to property change events for {@link ChangesetCacheManagerModel#CHANGESET_IN_DETAIL_VIEW_PROP}
 * and updates its view accordingly.
 *
 */
public class ChangesetContentPanel extends JPanel implements PropertyChangeListener {

    private ChangesetContentTableModel model;
    private Changeset currentChangeset;

    private DownloadChangesetContentAction actDownloadContentAction;
    private ShowHistoryAction actShowHistory;
    private SelectInCurrentLayerAction actSelectInCurrentLayerAction;
    private ZoomInCurrentLayerAction actZoomInCurrentLayerAction;

    private final HeaderPanel pnlHeader = new HeaderPanel();

    protected void buildModels() {
        DefaultListSelectionModel selectionModel =new DefaultListSelectionModel();
        model = new ChangesetContentTableModel(selectionModel);
        actDownloadContentAction = new DownloadChangesetContentAction();
        actDownloadContentAction.initProperties(currentChangeset);
        actShowHistory = new ShowHistoryAction();
        model.getSelectionModel().addListSelectionListener(actShowHistory);

        actSelectInCurrentLayerAction = new SelectInCurrentLayerAction();
        model.getSelectionModel().addListSelectionListener(actSelectInCurrentLayerAction);
        MapView.addEditLayerChangeListener(actSelectInCurrentLayerAction);

        actZoomInCurrentLayerAction = new ZoomInCurrentLayerAction();
        model.getSelectionModel().addListSelectionListener(actZoomInCurrentLayerAction);
        MapView.addEditLayerChangeListener(actZoomInCurrentLayerAction);

        addComponentListener(
                new ComponentAdapter() {
                    @Override
                    public void componentHidden(ComponentEvent e) {
                        // make sure the listener is unregistered when the panel becomes
                        // invisible
                        MapView.removeEditLayerChangeListener(actSelectInCurrentLayerAction);
                        MapView.removeEditLayerChangeListener(actZoomInCurrentLayerAction);
                    }
                }
        );
    }

    protected JPanel buildContentPanel() {
        JPanel pnl = new JPanel(new BorderLayout());
        JTable tblContent = new JTable(
                model,
                new ChangesetContentTableColumnModel(),
                model.getSelectionModel()
        );
        tblContent.addMouseListener(new PopupMenuLauncher(new ChangesetContentTablePopupMenu()));
        pnl.add(new JScrollPane(tblContent), BorderLayout.CENTER);
        return pnl;
    }

    protected JPanel buildActionButtonPanel() {
        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JToolBar tb = new JToolBar(JToolBar.VERTICAL);
        tb.setFloatable(false);

        tb.add(actDownloadContentAction);
        tb.add(actShowHistory);
        tb.add(actSelectInCurrentLayerAction);
        tb.add(actZoomInCurrentLayerAction);

        pnl.add(tb);
        return pnl;
    }

    protected final void build() {
        setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        setLayout(new BorderLayout());
        buildModels();

        add(pnlHeader, BorderLayout.NORTH);
        add(buildActionButtonPanel(), BorderLayout.WEST);
        add(buildContentPanel(), BorderLayout.CENTER);
    }

    /**
     * Constructs a new {@code ChangesetContentPanel}.
     */
    public ChangesetContentPanel() {
        build();
    }

    /**
     * Replies the changeset content model
     * @return The model
     */
    public ChangesetContentTableModel getModel() {
        return model;
    }

    protected void setCurrentChangeset(Changeset cs) {
        currentChangeset = cs;
        if (cs == null) {
            model.populate(null);
        } else {
            model.populate(cs.getContent());
        }
        actDownloadContentAction.initProperties(cs);
        pnlHeader.setChangeset(cs);
    }

    /* ---------------------------------------------------------------------------- */
    /* interface PropertyChangeListener                                             */
    /* ---------------------------------------------------------------------------- */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if(!evt.getPropertyName().equals(ChangesetCacheManagerModel.CHANGESET_IN_DETAIL_VIEW_PROP))
            return;
        Changeset cs = (Changeset)evt.getNewValue();
        setCurrentChangeset(cs);
    }
    
    private final void alertNoPrimitivesTo(Collection<HistoryOsmPrimitive> primitives, String title, String helpTopic) {
        HelpAwareOptionPane.showOptionDialog(
                ChangesetContentPanel.this,
                trn("<html>The selected object is not available in the current<br>"
                        + "edit layer ''{0}''.</html>",
                        "<html>None of the selected objects is available in the current<br>"
                        + "edit layer ''{0}''.</html>",
                        primitives.size(),
                        Main.main.getEditLayer().getName()
                ),
                title, JOptionPane.WARNING_MESSAGE, helpTopic
        );
    }

    /**
     * Downloads/Updates the content of the changeset
     *
     */
    class DownloadChangesetContentAction extends AbstractAction{
        public DownloadChangesetContentAction() {
            putValue(NAME, tr("Download content"));
            putValue(SMALL_ICON, ChangesetCacheManager.DOWNLOAD_CONTENT_ICON);
            putValue(SHORT_DESCRIPTION, tr("Download the changeset content from the OSM server"));
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            if (currentChangeset == null) return;
            ChangesetContentDownloadTask task = new ChangesetContentDownloadTask(ChangesetContentPanel.this,currentChangeset.getId());
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

    class ChangesetContentTablePopupMenu extends JPopupMenu {
        public ChangesetContentTablePopupMenu() {
            add(actDownloadContentAction);
            add(actShowHistory);
            add(new JSeparator());
            add(actSelectInCurrentLayerAction);
            add(actZoomInCurrentLayerAction);
        }
    }

    class ShowHistoryAction extends AbstractAction implements ListSelectionListener {
        
        private final class ShowHistoryTask implements Runnable {
            private final Collection<HistoryOsmPrimitive> primitives;

            private ShowHistoryTask(Collection<HistoryOsmPrimitive> primitives) {
                this.primitives = primitives;
            }

            @Override
            public void run() {
                try {
                    for (HistoryOsmPrimitive p : primitives) {
                        final History h = HistoryDataSet.getInstance().getHistory(p.getPrimitiveId());
                        if (h == null) {
                            continue;
                        }
                        GuiHelper.runInEDT(new Runnable() {
                            @Override
                            public void run() {
                                HistoryBrowserDialogManager.getInstance().show(h);
                            }
                        });
                    }
                } catch (final Exception e) {
                    GuiHelper.runInEDT(new Runnable() {
                        @Override
                        public void run() {
                            BugReportExceptionHandler.handleException(e);
                        }
                    });
                }
            }
        }

        public ShowHistoryAction() {
            putValue(NAME, tr("Show history"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "history"));
            putValue(SHORT_DESCRIPTION, tr("Download and show the history of the selected objects"));
            updateEnabledState();
        }

        protected List<HistoryOsmPrimitive> filterPrimitivesWithUnloadedHistory(Collection<HistoryOsmPrimitive> primitives) {
            List<HistoryOsmPrimitive> ret = new ArrayList<HistoryOsmPrimitive>(primitives.size());
            for (HistoryOsmPrimitive p: primitives) {
                if (HistoryDataSet.getInstance().getHistory(p.getPrimitiveId()) == null) {
                    ret.add(p);
                }
            }
            return ret;
        }

        public void showHistory(final Collection<HistoryOsmPrimitive> primitives) {

            List<HistoryOsmPrimitive> toLoad = filterPrimitivesWithUnloadedHistory(primitives);
            if (!toLoad.isEmpty()) {
                HistoryLoadTask task = new HistoryLoadTask(ChangesetContentPanel.this);
                for (HistoryOsmPrimitive p: toLoad) {
                    task.add(p);
                }
                Main.worker.submit(task);
            }

            Main.worker.submit(new ShowHistoryTask(primitives));
        }

        protected final void updateEnabledState() {
            setEnabled(model.hasSelectedPrimitives());
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            Set<HistoryOsmPrimitive> selected = model.getSelectedPrimitives();
            if (selected.isEmpty()) return;
            showHistory(selected);
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }
    }

    class SelectInCurrentLayerAction extends AbstractAction implements ListSelectionListener, EditLayerChangeListener{

        public SelectInCurrentLayerAction() {
            putValue(NAME, tr("Select in layer"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "select"));
            putValue(SHORT_DESCRIPTION, tr("Select the corresponding primitives in the current data layer"));
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (!isEnabled())
                return;
            if (Main.main == null || !Main.main.hasEditLayer()) return;
            OsmDataLayer layer = Main.main.getEditLayer();
            Set<HistoryOsmPrimitive> selected = model.getSelectedPrimitives();
            Set<OsmPrimitive> target = new HashSet<OsmPrimitive>();
            for (HistoryOsmPrimitive p : model.getSelectedPrimitives()) {
                OsmPrimitive op = layer.data.getPrimitiveById(p.getPrimitiveId());
                if (op != null) {
                    target.add(op);
                }
            }
            if (target.isEmpty()) {
                alertNoPrimitivesTo(selected, tr("Nothing to select"), 
                        HelpUtil.ht("/Dialog/ChangesetCacheManager#NothingToSelectInLayer"));
                return;
            }
            layer.data.setSelected(target);
        }

        public final void updateEnabledState() {
            if (Main.main == null || !Main.main.hasEditLayer()) {
                setEnabled(false);
                return;
            }
            setEnabled(model.hasSelectedPrimitives());
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }

        @Override
        public void editLayerChanged(OsmDataLayer oldLayer, OsmDataLayer newLayer) {
            updateEnabledState();
        }
    }

    class ZoomInCurrentLayerAction extends AbstractAction implements ListSelectionListener, EditLayerChangeListener{

        public ZoomInCurrentLayerAction() {
            putValue(NAME, tr("Zoom to in layer"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs/autoscale", "selection"));
            putValue(SHORT_DESCRIPTION, tr("Zoom to the corresponding objects in the current data layer"));
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (!isEnabled())
                return;
            if (Main.main == null || !Main.main.hasEditLayer()) return;
            OsmDataLayer layer = Main.main.getEditLayer();
            Set<HistoryOsmPrimitive> selected = model.getSelectedPrimitives();
            Set<OsmPrimitive> target = new HashSet<OsmPrimitive>();
            for (HistoryOsmPrimitive p : model.getSelectedPrimitives()) {
                OsmPrimitive op = layer.data.getPrimitiveById(p.getPrimitiveId());
                if (op != null) {
                    target.add(op);
                }
            }
            if (target.isEmpty()) {
                alertNoPrimitivesTo(selected, tr("Nothing to zoom to"), 
                        HelpUtil.ht("/Dialog/ChangesetCacheManager#NothingToZoomTo"));
                return;
            }
            layer.data.setSelected(target);
            AutoScaleAction.zoomToSelection();
        }

        public final void updateEnabledState() {
            if (Main.main == null || !Main.main.hasEditLayer()) {
                setEnabled(false);
                return;
            }
            setEnabled(model.hasSelectedPrimitives());
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }

        @Override
        public void editLayerChanged(OsmDataLayer oldLayer, OsmDataLayer newLayer) {
            updateEnabledState();
        }
    }

    private static class HeaderPanel extends JPanel {

        private JMultilineLabel lblMessage;
        private Changeset current;

        protected final void build() {
            setLayout(new FlowLayout(FlowLayout.LEFT));
            lblMessage = new JMultilineLabel(
                    tr("The content of this changeset is not downloaded yet.")
            );
            add(lblMessage);
            add(new JButton(new DownloadAction()));

        }

        public HeaderPanel() {
            build();
        }

        public void setChangeset(Changeset cs) {
            setVisible(cs != null && cs.getContent() == null);
            this.current = cs;
        }

        private class DownloadAction extends AbstractAction {
            public DownloadAction() {
                putValue(NAME, tr("Download now"));
                putValue(SHORT_DESCRIPTION, tr("Download the changeset content"));
                putValue(SMALL_ICON, ChangesetCacheManager.DOWNLOAD_CONTENT_ICON);
            }

            @Override
            public void actionPerformed(ActionEvent evt) {
                if (current == null) return;
                ChangesetContentDownloadTask task = new ChangesetContentDownloadTask(HeaderPanel.this, current.getId());
                ChangesetCacheManager.getInstance().runDownloadTask(task);
            }
        }
    }
}
