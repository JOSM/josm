// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import javax.swing.SwingUtilities;
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
import org.openstreetmap.josm.gui.JMultilineLabel;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapView.EditLayerChangeListener;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.history.HistoryBrowserDialogManager;
import org.openstreetmap.josm.gui.history.HistoryLoadTask;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.BugReportExceptionHandler;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * The panel which displays the content of a changeset in a scollable table.
 *
 * It listens to property change events for {@see ChangesetCacheManagerModel#CHANGESET_IN_DETAIL_VIEW_PROP}
 * and updates its view accordingly.
 *
 */
public class ChangesetContentPanel extends JPanel implements PropertyChangeListener{

    private ChangesetContentTableModel model;
    private Changeset currentChangeset;
    private JTable tblContent;

    private DonwloadChangesetContentAction actDownloadContentAction;
    private ShowHistoryAction actShowHistory;
    private SelectInCurrentLayerAction actSelectInCurrentLayerAction;
    private ZoomInCurrentLayerAction actZoomInCurrentLayerAction;

    private HeaderPanel pnlHeader;

    protected void buildModels() {
        DefaultListSelectionModel selectionModel =new DefaultListSelectionModel();
        model = new ChangesetContentTableModel(selectionModel);
        actDownloadContentAction = new DonwloadChangesetContentAction();
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
        tblContent = new JTable(
                model,
                new ChangesetContentTableColumnModel(),
                model.getSelectionModel()
        );
        tblContent.addMouseListener(new ChangesetContentTablePopupMenuLauncher());
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

    protected void build() {
        setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        setLayout(new BorderLayout());
        buildModels();

        add(pnlHeader = new HeaderPanel(), BorderLayout.NORTH);
        add(buildActionButtonPanel(), BorderLayout.WEST);
        add(buildContentPanel(), BorderLayout.CENTER);

    }

    public ChangesetContentPanel() {
        build();
    }

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
    public void propertyChange(PropertyChangeEvent evt) {
        if(!evt.getPropertyName().equals(ChangesetCacheManagerModel.CHANGESET_IN_DETAIL_VIEW_PROP))
            return;
        Changeset cs = (Changeset)evt.getNewValue();
        setCurrentChangeset(cs);
    }

    /**
     * Downloads/Updates the content of the changeset
     *
     */
    class DonwloadChangesetContentAction extends AbstractAction{
        public DonwloadChangesetContentAction() {
            putValue(NAME, tr("Download content"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs/changeset","downloadchangesetcontent"));
            putValue(SHORT_DESCRIPTION, tr("Download the changeset content from the OSM server"));
        }

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
                putValue(SMALL_ICON, ImageProvider.get("dialogs/changeset","downloadchangesetcontent"));
                putValue(SHORT_DESCRIPTION, tr("Download the changeset content from the OSM server"));
            } else {
                putValue(NAME, tr("Update content"));
                putValue(SMALL_ICON, ImageProvider.get("dialogs/changeset","updatechangesetcontent"));
                putValue(SHORT_DESCRIPTION, tr("Update the changeset content from the OSM server"));
            }
        }
    }

    class ChangesetContentTablePopupMenuLauncher extends MouseAdapter {
        ChangesetContentTablePopupMenu menu = new ChangesetContentTablePopupMenu();

        protected void launch(MouseEvent evt) {
            if (! evt.isPopupTrigger()) return;
            if (! model.hasSelectedPrimitives()) {
                int row = tblContent.rowAtPoint(evt.getPoint());
                if (row >= 0) {
                    model.setSelectedByIdx(row);
                }
            }
            menu.show(tblContent, evt.getPoint().x, evt.getPoint().y);
        }

        @Override
        public void mouseClicked(MouseEvent evt) {
            launch(evt);
        }

        @Override
        public void mousePressed(MouseEvent evt) {
            launch(evt);
        }

        @Override
        public void mouseReleased(MouseEvent evt) {
            launch(evt);
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

    class ShowHistoryAction extends AbstractAction implements ListSelectionListener{
        public ShowHistoryAction() {
            putValue(NAME, tr("Show history"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "history"));
            putValue(SHORT_DESCRIPTION, tr("Download and show the history of the selected primitives"));
            updateEnabledState();
        }

        protected List<HistoryOsmPrimitive> filterPrimitivesWithUnloadedHistory(Collection<HistoryOsmPrimitive> primitives) {
            ArrayList<HistoryOsmPrimitive> ret = new ArrayList<HistoryOsmPrimitive>(primitives.size());
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

            Runnable r = new Runnable() {
                public void run() {
                    try {
                        for (HistoryOsmPrimitive p : primitives) {
                            History h = HistoryDataSet.getInstance().getHistory(p.getPrimitiveId());
                            if (h == null) {
                                continue;
                            }
                            HistoryBrowserDialogManager.getInstance().show(h);
                        }
                    } catch (final Exception e) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                BugReportExceptionHandler.handleException(e);
                            }
                        });
                    }

                }
            };
            Main.worker.submit(r);
        }

        protected void updateEnabledState() {
            setEnabled(model.hasSelectedPrimitives());
        }

        public void actionPerformed(ActionEvent arg0) {
            Set<HistoryOsmPrimitive> selected = model.getSelectedPrimitives();
            if (selected.isEmpty()) return;
            showHistory(selected);
        }

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

        protected void alertNoPrimitivesToSelect(Collection<HistoryOsmPrimitive> primitives) {
            HelpAwareOptionPane.showOptionDialog(
                    ChangesetContentPanel.this,
                    trn("<html>The selected object is not available in the current<br>"
                            + "edit layer ''{0}''.</html>",
                            "<html>None of the selected objects is available in the current<br>"
                            + "edit layer ''{0}''.</html>",
                            primitives.size(),
                            Main.main.getEditLayer().getName()
                    ),
                    tr("Nothing to select"),
                    JOptionPane.WARNING_MESSAGE,
                    HelpUtil.ht("/Dialog/ChangesetCacheManager#NothingToSelectInLayer")
            );
        }

        public void actionPerformed(ActionEvent arg0) {
            if (!isEnabled())
                return;
            if (Main.main == null || Main.main.getEditLayer() == null) return;
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
                alertNoPrimitivesToSelect(selected);
                return;
            }
            layer.data.setSelected(target);
        }

        public void updateEnabledState() {
            if (Main.main == null || Main.main.getEditLayer() == null){
                setEnabled(false);
                return;
            }
            setEnabled(model.hasSelectedPrimitives());
        }

        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }

        public void editLayerChanged(OsmDataLayer oldLayer, OsmDataLayer newLayer) {
            updateEnabledState();
        }
    }

    class ZoomInCurrentLayerAction extends AbstractAction implements ListSelectionListener, EditLayerChangeListener{

        public ZoomInCurrentLayerAction() {
            putValue(NAME, tr("Zoom to in layer"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs/autoscale", "selection"));
            putValue(SHORT_DESCRIPTION, tr("Zoom to the corresponding primitives in the current data layer"));
            updateEnabledState();
        }

        protected void alertNoPrimitivesToZoomTo(Collection<HistoryOsmPrimitive> primitives) {
            HelpAwareOptionPane.showOptionDialog(
                    ChangesetContentPanel.this,
                    trn("<html>The selected object is not available in the current<br>"
                            + "edit layer ''{0}''.</html>",
                            "<html>None of the selected objects is available in the current<br>"
                            + "edit layer ''{0}''.</html>",
                            primitives.size(),
                            Main.main.getEditLayer().getName()
                    ),
                    tr("Nothing to zoom to"),
                    JOptionPane.WARNING_MESSAGE,
                    HelpUtil.ht("/Dialog/ChangesetCacheManager#NothingToZoomTo")
            );
        }

        public void actionPerformed(ActionEvent arg0) {
            if (!isEnabled())
                return;
            if (Main.main == null || Main.main.getEditLayer() == null) return;
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
                alertNoPrimitivesToZoomTo(selected);
                return;
            }
            layer.data.setSelected(target);
            AutoScaleAction.zoomToSelection();
        }

        public void updateEnabledState() {
            if (Main.main == null || Main.main.getEditLayer() == null){
                setEnabled(false);
                return;
            }
            setEnabled(model.hasSelectedPrimitives());
        }

        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }

        public void editLayerChanged(OsmDataLayer oldLayer, OsmDataLayer newLayer) {
            updateEnabledState();
        }
    }

    static private class HeaderPanel extends JPanel {

        private JMultilineLabel lblMessage;
        private Changeset current;

        protected void build() {
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
                putValue(SMALL_ICON, ImageProvider.get("dialogs/changeset", "downloadchangesetcontent"));
            }

            public void actionPerformed(ActionEvent evt) {
                if (current == null) return;
                ChangesetContentDownloadTask task = new ChangesetContentDownloadTask(HeaderPanel.this, current.getId());
                ChangesetCacheManager.getInstance().runDownloadTask(task);
            }
        }
    }
}
