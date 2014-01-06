// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AbstractInfoAction;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.ChangesetCache;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.event.DatasetEventManager;
import org.openstreetmap.josm.data.osm.event.DatasetEventManager.FireMode;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.dialogs.changeset.ChangesetCacheManager;
import org.openstreetmap.josm.gui.dialogs.changeset.ChangesetHeaderDownloadTask;
import org.openstreetmap.josm.gui.dialogs.changeset.ChangesetInSelectionListModel;
import org.openstreetmap.josm.gui.dialogs.changeset.ChangesetListCellRenderer;
import org.openstreetmap.josm.gui.dialogs.changeset.ChangesetListModel;
import org.openstreetmap.josm.gui.dialogs.changeset.ChangesetsInActiveDataLayerListModel;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.io.CloseChangesetTask;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.widgets.ListPopupMenu;
import org.openstreetmap.josm.gui.widgets.PopupMenuLauncher;
import org.openstreetmap.josm.tools.BugReportExceptionHandler;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.OpenBrowser;

/**
 * ChangesetDialog is a toggle dialog which displays the current list of changesets.
 * It either displays
 * <ul>
 *   <li>the list of changesets the currently selected objects are assigned to</li>
 *   <li>the list of changesets objects in the current data layer are assigend to</li>
 * </ul>
 *
 * The dialog offers actions to download and to close changesets. It can also launch an external
 * browser with information about a changeset. Furthermore, it can select all objects in
 * the current data layer being assigned to a specific changeset.
 *
 */
public class ChangesetDialog extends ToggleDialog{
    private ChangesetInSelectionListModel inSelectionModel;
    private ChangesetsInActiveDataLayerListModel inActiveDataLayerModel;
    private JList lstInSelection;
    private JList lstInActiveDataLayer;
    private JCheckBox cbInSelectionOnly;
    private JPanel pnlList;

    // the actions
    private SelectObjectsAction selectObjectsAction;
    private ReadChangesetsAction readChangesetAction;
    private ShowChangesetInfoAction showChangesetInfoAction;
    private CloseOpenChangesetsAction closeChangesetAction;
    private LaunchChangesetManagerAction launchChangesetManagerAction;

    private ChangesetDialogPopup popupMenu;

    protected void buildChangesetsLists() {
        DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();
        inSelectionModel = new ChangesetInSelectionListModel(selectionModel);

        lstInSelection = new JList(inSelectionModel);
        lstInSelection.setSelectionModel(selectionModel);
        lstInSelection.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        lstInSelection.setCellRenderer(new ChangesetListCellRenderer());

        selectionModel = new DefaultListSelectionModel();
        inActiveDataLayerModel = new ChangesetsInActiveDataLayerListModel(selectionModel);
        lstInActiveDataLayer = new JList(inActiveDataLayerModel);
        lstInActiveDataLayer.setSelectionModel(selectionModel);
        lstInActiveDataLayer.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        lstInActiveDataLayer.setCellRenderer(new ChangesetListCellRenderer());

        DblClickHandler dblClickHandler = new DblClickHandler();
        lstInSelection.addMouseListener(dblClickHandler);
        lstInActiveDataLayer.addMouseListener(dblClickHandler);
    }

    protected void registerAsListener() {
        // let the model for changesets in the current selection listen to various
        // events
        ChangesetCache.getInstance().addChangesetCacheListener(inSelectionModel);
        MapView.addEditLayerChangeListener(inSelectionModel);
        DataSet.addSelectionListener(inSelectionModel);

        // let the model for changesets in the current layer listen to various
        // events and bootstrap it's content
        ChangesetCache.getInstance().addChangesetCacheListener(inActiveDataLayerModel);
        MapView.addEditLayerChangeListener(inActiveDataLayerModel);
        OsmDataLayer editLayer = Main.main.getEditLayer();
        if (editLayer != null) {
            editLayer.data.addDataSetListener(inActiveDataLayerModel);
            inActiveDataLayerModel.initFromDataSet(editLayer.data);
            inSelectionModel.initFromPrimitives(editLayer.data.getAllSelected());
        }
    }

    protected void unregisterAsListener() {
        // remove the list model for the current edit layer as listener
        //
        ChangesetCache.getInstance().removeChangesetCacheListener(inActiveDataLayerModel);
        MapView.removeEditLayerChangeListener(inActiveDataLayerModel);
        OsmDataLayer editLayer = Main.main.getEditLayer();
        if (editLayer != null) {
            editLayer.data.removeDataSetListener(inActiveDataLayerModel);
        }

        // remove the list model for the changesets in the current selection as
        // listener
        //
        MapView.removeEditLayerChangeListener(inSelectionModel);
        DataSet.removeSelectionListener(inSelectionModel);
    }

    @Override
    public void showNotify() {
        registerAsListener();
        DatasetEventManager.getInstance().addDatasetListener(inActiveDataLayerModel, FireMode.IN_EDT);
    }

    @Override
    public void hideNotify() {
        unregisterAsListener();
        DatasetEventManager.getInstance().removeDatasetListener(inActiveDataLayerModel);
    }

    protected JPanel buildFilterPanel() {
        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnl.setBorder(null);
        pnl.add(cbInSelectionOnly = new JCheckBox(tr("For selected objects only")));
        cbInSelectionOnly.setToolTipText(tr("<html>Select to show changesets for the currently selected objects only.<br>"
                + "Unselect to show all changesets for objects in the current data layer.</html>"));
        cbInSelectionOnly.setSelected(Main.pref.getBoolean("changeset-dialog.for-selected-objects-only", false));
        return pnl;
    }

    protected JPanel buildListPanel() {
        buildChangesetsLists();
        JPanel pnl = new JPanel(new BorderLayout());
        if (cbInSelectionOnly.isSelected()) {
            pnl.add(new JScrollPane(lstInSelection));
        } else {
            pnl.add(new JScrollPane(lstInActiveDataLayer));
        }
        return pnl;
    }

    protected void build() {
        JPanel pnl = new JPanel(new BorderLayout());
        pnl.add(buildFilterPanel(), BorderLayout.NORTH);
        pnl.add(pnlList = buildListPanel(), BorderLayout.CENTER);

        cbInSelectionOnly.addItemListener(new FilterChangeHandler());

        HelpUtil.setHelpContext(pnl, HelpUtil.ht("/Dialog/ChangesetListDialog"));

        // -- select objects action
        selectObjectsAction = new SelectObjectsAction();
        cbInSelectionOnly.addItemListener(selectObjectsAction);

        // -- read changesets action
        readChangesetAction = new ReadChangesetsAction();
        cbInSelectionOnly.addItemListener(readChangesetAction);

        // -- close changesets action
        closeChangesetAction = new CloseOpenChangesetsAction();
        cbInSelectionOnly.addItemListener(closeChangesetAction);

        // -- show info action
        showChangesetInfoAction = new ShowChangesetInfoAction();
        cbInSelectionOnly.addItemListener(showChangesetInfoAction);

        // -- launch changeset manager action
        launchChangesetManagerAction = new LaunchChangesetManagerAction();
        cbInSelectionOnly.addItemListener(launchChangesetManagerAction);

        popupMenu = new ChangesetDialogPopup(lstInActiveDataLayer, lstInSelection);

        PopupMenuLauncher popupMenuLauncher = new PopupMenuLauncher(popupMenu);
        lstInSelection.addMouseListener(popupMenuLauncher);
        lstInActiveDataLayer.addMouseListener(popupMenuLauncher);

        createLayout(pnl, false, Arrays.asList(new SideButton[] {
            new SideButton(selectObjectsAction, false),
            new SideButton(readChangesetAction, false),
            new SideButton(closeChangesetAction, false),
            new SideButton(showChangesetInfoAction, false),
            new SideButton(launchChangesetManagerAction, false)
        }));
    }

    protected JList getCurrentChangesetList() {
        if (cbInSelectionOnly.isSelected())
            return lstInSelection;
        return lstInActiveDataLayer;
    }

    protected ChangesetListModel getCurrentChangesetListModel() {
        if (cbInSelectionOnly.isSelected())
            return inSelectionModel;
        return inActiveDataLayerModel;
    }

    protected void initWithCurrentData() {
        OsmDataLayer editLayer = Main.main.getEditLayer();
        if (editLayer != null) {
            inSelectionModel.initFromPrimitives(editLayer.data.getAllSelected());
            inActiveDataLayerModel.initFromDataSet(editLayer.data);
        }
    }

    /**
     * Constructs a new {@code ChangesetDialog}.
     */
    public ChangesetDialog() {
        super(
                tr("Changesets"),
                "changesetdialog",
                tr("Open the list of changesets in the current layer."),
                null, /* no keyboard shortcut */
                200, /* the preferred height */
                false /* don't show if there is no preference */
        );
        build();
        initWithCurrentData();
    }

    class DblClickHandler extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (!SwingUtilities.isLeftMouseButton(e) || e.getClickCount() < 2)
                return;
            Set<Integer> sel = getCurrentChangesetListModel().getSelectedChangesetIds();
            if (sel.isEmpty())
                return;
            if (Main.main.getCurrentDataSet() == null)
                return;
            new SelectObjectsAction().selectObjectsByChangesetIds(Main.main.getCurrentDataSet(), sel);
        }

    }

    class FilterChangeHandler implements ItemListener {
        @Override
        public void itemStateChanged(ItemEvent e) {
            Main.pref.put("changeset-dialog.for-selected-objects-only", cbInSelectionOnly.isSelected());
            pnlList.removeAll();
            if (cbInSelectionOnly.isSelected()) {
                pnlList.add(new JScrollPane(lstInSelection), BorderLayout.CENTER);
            } else {
                pnlList.add(new JScrollPane(lstInActiveDataLayer), BorderLayout.CENTER);
            }
            validate();
            repaint();
        }
    }

    /**
     * Selects objects for the currently selected changesets.
     */
    class SelectObjectsAction extends AbstractAction implements ListSelectionListener, ItemListener{

        public SelectObjectsAction() {
            putValue(NAME, tr("Select"));
            putValue(SHORT_DESCRIPTION, tr("Select all objects assigned to the currently selected changesets"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "select"));
            updateEnabledState();
        }

        public void selectObjectsByChangesetIds(DataSet ds, Set<Integer> ids) {
            if (ds == null || ids == null)
                return;
            Set<OsmPrimitive> sel = new HashSet<OsmPrimitive>();
            for (OsmPrimitive p: ds.allPrimitives()) {
                if (ids.contains(p.getChangesetId())) {
                    sel.add(p);
                }
            }
            ds.setSelected(sel);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!Main.main.hasEditLayer())
                return;
            ChangesetListModel model = getCurrentChangesetListModel();
            Set<Integer> sel = model.getSelectedChangesetIds();
            if (sel.isEmpty())
                return;

            DataSet ds = Main.main.getEditLayer().data;
            selectObjectsByChangesetIds(ds,sel);
        }

        protected void updateEnabledState() {
            setEnabled(getCurrentChangesetList().getSelectedIndices().length > 0);
        }

        @Override
        public void itemStateChanged(ItemEvent arg0) {
            updateEnabledState();

        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }
    }

    /**
     * Downloads selected changesets
     *
     */
    class ReadChangesetsAction extends AbstractAction implements ListSelectionListener, ItemListener{
        public ReadChangesetsAction() {
            putValue(NAME, tr("Download"));
            putValue(SHORT_DESCRIPTION, tr("Download information about the selected changesets from the OSM server"));
            putValue(SMALL_ICON, ImageProvider.get("download"));
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            ChangesetListModel model = getCurrentChangesetListModel();
            Set<Integer> sel = model.getSelectedChangesetIds();
            if (sel.isEmpty())
                return;
            ChangesetHeaderDownloadTask task = new ChangesetHeaderDownloadTask(sel);
            Main.worker.submit(task);
        }

        protected void updateEnabledState() {
            setEnabled(getCurrentChangesetList().getSelectedIndices().length > 0);
        }

        @Override
        public void itemStateChanged(ItemEvent arg0) {
            updateEnabledState();

        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }
    }

    /**
     * Closes the currently selected changesets
     *
     */
    class CloseOpenChangesetsAction extends AbstractAction implements ListSelectionListener, ItemListener {
        public CloseOpenChangesetsAction() {
            putValue(NAME, tr("Close open changesets"));
            putValue(SHORT_DESCRIPTION, tr("Closes the selected open changesets"));
            putValue(SMALL_ICON, ImageProvider.get("closechangeset"));
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            List<Changeset> sel = getCurrentChangesetListModel().getSelectedOpenChangesets();
            if (sel.isEmpty())
                return;
            Main.worker.submit(new CloseChangesetTask(sel));
        }

        protected void updateEnabledState() {
            setEnabled(getCurrentChangesetListModel().hasSelectedOpenChangesets());
        }

        @Override
        public void itemStateChanged(ItemEvent arg0) {
            updateEnabledState();
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }
    }

    /**
     * Show information about the currently selected changesets
     *
     */
    class ShowChangesetInfoAction extends AbstractAction implements ListSelectionListener, ItemListener {
        public ShowChangesetInfoAction() {
            putValue(NAME, tr("Show info"));
            putValue(SHORT_DESCRIPTION, tr("Open a web page for each selected changeset"));
            putValue(SMALL_ICON, ImageProvider.get("about"));
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            Set<Changeset> sel = getCurrentChangesetListModel().getSelectedChangesets();
            if (sel.isEmpty())
                return;
            if (sel.size() > 10 && ! AbstractInfoAction.confirmLaunchMultiple(sel.size()))
                return;
            String baseUrl = AbstractInfoAction.getBaseBrowseUrl();
            for (Changeset cs: sel) {
                String url = baseUrl + "/changeset/" + cs.getId();
                OpenBrowser.displayUrl(
                        url
                );
            }
        }

        protected void updateEnabledState() {
            setEnabled(getCurrentChangesetList().getSelectedIndices().length > 0);
        }

        @Override
        public void itemStateChanged(ItemEvent arg0) {
            updateEnabledState();
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }
    }

    /**
     * Show information about the currently selected changesets
     *
     */
    class LaunchChangesetManagerAction extends AbstractAction implements ListSelectionListener, ItemListener {
        public LaunchChangesetManagerAction() {
            putValue(NAME, tr("Details"));
            putValue(SHORT_DESCRIPTION, tr("Opens the Changeset Manager window for the selected changesets"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs/changeset", "changesetmanager"));
        }

        protected void launchChangesetManager(Collection<Integer> toSelect) {
            ChangesetCacheManager cm = ChangesetCacheManager.getInstance();
            if (cm.isVisible()) {
                cm.setExtendedState(Frame.NORMAL);
                cm.toFront();
                cm.requestFocus();
            } else {
                cm.setVisible(true);
                cm.toFront();
                cm.requestFocus();
            }
            cm.setSelectedChangesetsById(toSelect);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            ChangesetListModel model = getCurrentChangesetListModel();
            Set<Integer> sel = model.getSelectedChangesetIds();
            final Set<Integer> toDownload = new HashSet<Integer>();
            ChangesetCache cc = ChangesetCache.getInstance();
            for (int id: sel) {
                if (!cc.contains(id)) {
                    toDownload.add(id);
                }
            }

            final ChangesetHeaderDownloadTask task;
            final Future<?> future;
            if (toDownload.isEmpty()) {
                task = null;
                future = null;
            } else {
                task = new ChangesetHeaderDownloadTask(toDownload);
                future = Main.worker.submit(task);
            }

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    // first, wait for the download task to finish, if a download
                    // task was launched
                    if (future != null) {
                        try {
                            future.get();
                        } catch(InterruptedException e) {
                            Main.warn("InterruptedException in "+getClass().getSimpleName()+" while downloading changeset header");
                        } catch(ExecutionException e) {
                            Main.error(e);
                            BugReportExceptionHandler.handleException(e.getCause());
                            return;
                        }
                    }
                    if (task != null) {
                        if (task.isCanceled())
                            // don't launch the changeset manager if the download task
                            // was canceled
                            return;
                        if (task.isFailed()) {
                            toDownload.clear();
                        }
                    }
                    // launch the task
                    launchChangesetManager(toDownload);
                }
            };
            Main.worker.submit(r);
        }

        @Override
        public void itemStateChanged(ItemEvent arg0) {
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
        }
    }

    class ChangesetDialogPopup extends ListPopupMenu {
        public ChangesetDialogPopup(JList ... lists) {
            super(lists);
            add(selectObjectsAction);
            addSeparator();
            add(readChangesetAction);
            add(closeChangesetAction);
            addSeparator();
            add(showChangesetInfoAction);
        }
    }

    public void addPopupMenuSeparator() {
        popupMenu.addSeparator();
    }

    public JMenuItem addPopupMenuAction(Action a) {
        return popupMenu.add(a);
    }
}
