// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
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
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.dialogs.changeset.ChangesetInSelectionListModel;
import org.openstreetmap.josm.gui.dialogs.changeset.ChangesetListCellRenderer;
import org.openstreetmap.josm.gui.dialogs.changeset.ChangesetListModel;
import org.openstreetmap.josm.gui.dialogs.changeset.ChangesetsInActiveDataLayerListModel;
import org.openstreetmap.josm.gui.dialogs.changeset.DownloadChangesetsTask;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.io.CloseChangesetTask;
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
    static private final Logger logger = Logger.getLogger(ChangesetDialog.class.getName());

    private ChangesetInSelectionListModel inSelectionModel;
    private ChangesetsInActiveDataLayerListModel inActiveDataLayerModel;
    private JList lstInSelection;
    private JList lstInActiveDataLayer;
    private JCheckBox cbInSelectionOnly;
    private JPanel pnlList;

    // the actions
    private SelectObjectsAction selectObjectsAction;
    private  ReadChangesetsAction readChangesetAction;
    private ShowChangesetInfoAction showChangsetInfoAction;
    private CloseOpenChangesetsAction closeChangesetAction;


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

        ChangesetCache.getInstance().addChangesetCacheListener(inSelectionModel);
        MapView.addLayerChangeListener(inSelectionModel);
        DataSet.selListeners.add(inSelectionModel);

        ChangesetCache.getInstance().addChangesetCacheListener(inActiveDataLayerModel);
        MapView.addLayerChangeListener(inActiveDataLayerModel);

        DblClickHandler dblClickHandler = new DblClickHandler();
        lstInSelection.addMouseListener(dblClickHandler);
        lstInActiveDataLayer.addMouseListener(dblClickHandler);

        PopupMenuLauncher popupMenuLauncher = new PopupMenuLauncher();
        lstInSelection.addMouseListener(popupMenuLauncher);
        lstInActiveDataLayer.addMouseListener(popupMenuLauncher);
    }

    @Override
    public void tearDown() {
        ChangesetCache.getInstance().removeChangesetCacheListener(inActiveDataLayerModel);
        MapView.removeLayerChangeListener(inSelectionModel);
        DataSet.selListeners.remove(inSelectionModel);
        MapView.removeLayerChangeListener(inActiveDataLayerModel);
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

    protected JPanel buildButtonPanel() {
        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JToolBar tp = new JToolBar(JToolBar.HORIZONTAL);
        tp.setFloatable(false);

        // -- select objects action
        selectObjectsAction = new SelectObjectsAction();
        tp.add(selectObjectsAction);
        cbInSelectionOnly.addItemListener(selectObjectsAction);
        lstInActiveDataLayer.getSelectionModel().addListSelectionListener(selectObjectsAction);
        lstInSelection.getSelectionModel().addListSelectionListener(selectObjectsAction);

        // -- read changesets action
        readChangesetAction = new ReadChangesetsAction();
        tp.add(readChangesetAction);
        cbInSelectionOnly.addItemListener(readChangesetAction);
        lstInActiveDataLayer.getSelectionModel().addListSelectionListener(readChangesetAction);
        lstInSelection.getSelectionModel().addListSelectionListener(readChangesetAction);

        // -- close changesets action
        closeChangesetAction = new CloseOpenChangesetsAction();
        tp.add(closeChangesetAction);
        cbInSelectionOnly.addItemListener(closeChangesetAction);
        lstInActiveDataLayer.getSelectionModel().addListSelectionListener(closeChangesetAction);
        lstInSelection.getSelectionModel().addListSelectionListener(closeChangesetAction);

        // -- show info action
        showChangsetInfoAction = new ShowChangesetInfoAction();
        tp.add(showChangsetInfoAction);
        cbInSelectionOnly.addItemListener(showChangsetInfoAction);
        lstInActiveDataLayer.getSelectionModel().addListSelectionListener(showChangsetInfoAction);
        lstInSelection.getSelectionModel().addListSelectionListener(showChangsetInfoAction);

        pnl.add(tp);
        return pnl;
    }

    protected void build() {
        JPanel pnl = new JPanel(new BorderLayout());
        pnl.add(buildFilterPanel(), BorderLayout.NORTH);
        pnl.add(pnlList = buildListPanel(), BorderLayout.CENTER);
        pnl.add(buildButtonPanel(), BorderLayout.SOUTH);
        add(pnl, BorderLayout.CENTER);

        cbInSelectionOnly.addItemListener(new FilterChangeHandler());

        HelpUtil.setHelpContext(pnl, HelpUtil.ht("/Dialog/ChangesetListDialog"));
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
        if (Main.main.getEditLayer() != null) {
            inSelectionModel.initFromPrimitives(Main.main.getEditLayer().data.getSelected());
            inActiveDataLayerModel.initFromDataSet(Main.main.getEditLayer().data);
        }
    }

    public ChangesetDialog(MapFrame mapFrame) {
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
            for (OsmPrimitive p: ds.getNodes()) {
                if (ids.contains(p.getChangesetId())) {
                    sel.add(p);
                }
            }
            for (OsmPrimitive p: ds.getWays()) {
                if (ids.contains(p.getChangesetId())) {
                    sel.add(p);
                }
            }
            for (OsmPrimitive p: ds.getRelations()) {
                if (ids.contains(p.getChangesetId())) {
                    sel.add(p);
                }
            }
            ds.setSelected(sel);
        }

        public void actionPerformed(ActionEvent e) {
            if (Main.main.getEditLayer() == null)
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

        public void itemStateChanged(ItemEvent arg0) {
            updateEnabledState();

        }

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

        public void actionPerformed(ActionEvent arg0) {
            ChangesetListModel model = getCurrentChangesetListModel();
            Set<Integer> sel = model.getSelectedChangesetIds();
            if (sel.isEmpty())
                return;
            DownloadChangesetsTask task = new DownloadChangesetsTask(sel);
            Main.worker.submit(task);
        }

        protected void updateEnabledState() {
            setEnabled(getCurrentChangesetList().getSelectedIndices().length > 0);
        }

        public void itemStateChanged(ItemEvent arg0) {
            updateEnabledState();

        }

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

        public void actionPerformed(ActionEvent arg0) {
            List<Changeset> sel = getCurrentChangesetListModel().getSelectedOpenChangesets();
            if (sel.isEmpty())
                return;
            Main.worker.submit(new CloseChangesetTask(sel));
        }

        protected void updateEnabledState() {
            setEnabled(getCurrentChangesetListModel().hasSelectedOpenChangesets());
        }

        public void itemStateChanged(ItemEvent arg0) {
            updateEnabledState();
        }

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

        public void itemStateChanged(ItemEvent arg0) {
            updateEnabledState();
        }

        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }
    }

    class PopupMenuLauncher extends MouseAdapter {
        protected void showPopup(MouseEvent evt) {
            if (!evt.isPopupTrigger())
                return;
            JList lst = getCurrentChangesetList();
            if (lst.getSelectedIndices().length == 0) {
                int idx = lst.locationToIndex(evt.getPoint());
                if (idx >=0) {
                    lst.getSelectionModel().addSelectionInterval(idx, idx);
                }
            }
            ChangesetDialogPopup popup = new ChangesetDialogPopup();
            popup.show(lst, evt.getX(), evt.getY());

        }
        @Override
        public void mouseClicked(MouseEvent evt) {
            showPopup(evt);
        }
        @Override
        public void mousePressed(MouseEvent evt) {
            showPopup(evt);
        }
        @Override
        public void mouseReleased(MouseEvent evt) {
            showPopup(evt);
        }
    }

    class ChangesetDialogPopup extends JPopupMenu {
        public ChangesetDialogPopup() {
            add(selectObjectsAction);
            addSeparator();
            add(readChangesetAction);
            add(closeChangesetAction);
            addSeparator();
            add(showChangsetInfoAction);
        }
    }
}
