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
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.ChangesetCache;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.dialogs.changeset.ChangesetInSelectionListModel;
import org.openstreetmap.josm.gui.dialogs.changeset.ChangesetListCellRenderer;
import org.openstreetmap.josm.gui.dialogs.changeset.ChangesetListModel;
import org.openstreetmap.josm.gui.dialogs.changeset.ChangesetsInActiveDataLayerListModel;
import org.openstreetmap.josm.gui.dialogs.changeset.DownloadChangesetsTask;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.tools.ImageProvider;

public class ChangesetDialog extends ToggleDialog{
    static private final Logger logger = Logger.getLogger(ChangesetDialog.class.getName());

    private ChangesetInSelectionListModel inSelectionModel;
    private ChangesetsInActiveDataLayerListModel inActiveDataLayerModel;
    private JList lstInSelection;
    private JList lstInActiveDataLayer;
    private JCheckBox cbInSelectionOnly;
    private JPanel pnlList;

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
        Layer.listeners.add(inSelectionModel);
        DataSet.selListeners.add(inSelectionModel);

        ChangesetCache.getInstance().addChangesetCacheListener(inActiveDataLayerModel);
        Layer.listeners.add(inActiveDataLayerModel);

        DblClickHandler dblClickHandler = new DblClickHandler();
        lstInSelection.addMouseListener(dblClickHandler);
        lstInActiveDataLayer.addMouseListener(dblClickHandler);
    }

    @Override
    public void tearDown() {
        ChangesetCache.getInstance().removeChangesetCacheListener(inActiveDataLayerModel);
        Layer.listeners.remove(inSelectionModel);
        DataSet.selListeners.remove(inSelectionModel);
        Layer.listeners.remove(inActiveDataLayerModel);
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

        SelectObjectsAction selectObjectsAction = new SelectObjectsAction();
        tp.add(selectObjectsAction);
        cbInSelectionOnly.addItemListener(selectObjectsAction);
        lstInActiveDataLayer.getSelectionModel().addListSelectionListener(selectObjectsAction);
        lstInSelection.getSelectionModel().addListSelectionListener(selectObjectsAction);

        ReadChangesetsAction readChangesetAction = new ReadChangesetsAction();
        tp.add(readChangesetAction);
        cbInSelectionOnly.addItemListener(readChangesetAction);
        lstInActiveDataLayer.getSelectionModel().addListSelectionListener(readChangesetAction);
        lstInSelection.getSelectionModel().addListSelectionListener(readChangesetAction);

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

}
