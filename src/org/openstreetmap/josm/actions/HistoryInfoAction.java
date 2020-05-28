// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JList;
import javax.swing.JTable;

import org.openstreetmap.josm.data.osm.OsmData;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.OsmIdSelectionDialog;
import org.openstreetmap.josm.gui.history.HistoryBrowserDialogManager;
import org.openstreetmap.josm.io.NetworkManager;
import org.openstreetmap.josm.io.OnlineResource;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Display history information about OSM ways, nodes, or relations.
 * @since 968
 */
public class HistoryInfoAction extends JosmAction {

    /** Action shortcut, made public in order to be used from {@code GettingStarted} page. */
    public static final Shortcut SHORTCUT = Shortcut.registerShortcut("core:historyinfo", tr("History"), KeyEvent.VK_H, Shortcut.CTRL);

    /**
     * Constructs a new {@code HistoryInfoAction}.
     */
    public HistoryInfoAction() {
        super(tr("History"), "dialogs/history",
                tr("Display history information about OSM ways, nodes, or relations."),
                SHORTCUT, true, "action/historyinfo", false);
        setHelpId(ht("/Action/ObjectHistory"));
        setEnabled(true);
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        // Generic handling of tables displaying OSM primitives
        Set<PrimitiveId> sel = new LinkedHashSet<>();
        if (ae.getSource() instanceof JTable) {
            JTable table = (JTable) ae.getSource();
            for (int row : table.getSelectedRows()) {
                for (int col = 0; col < table.getModel().getColumnCount(); col++) {
                    Object value = table.getModel().getValueAt(row, col);
                    if (value instanceof PrimitiveId) {
                        sel.add((PrimitiveId) value);
                        break;
                    }
                }
            }
        } else if (ae.getSource() instanceof JList) {
            JList<?> list = (JList<?>) ae.getSource();
            sel = list.getSelectedValuesList()
                    .stream().filter(v -> v instanceof PrimitiveId)
                    .map(v -> (PrimitiveId) v)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        if (!sel.isEmpty()) {
            HistoryBrowserDialogManager.getInstance().showHistory(sel);
            return;
        }
        // Otherwise show history for currently selected objects
        OsmData<?, ?, ?, ?> set = getLayerManager().getActiveData();
        if (set != null && !set.selectionEmpty()) {
            HistoryBrowserDialogManager.getInstance().showHistory(set.getAllSelected());
        } else {
            HistoryObjectIDDialog dialog = new HistoryObjectIDDialog();
            if (dialog.showDialog().getValue() == dialog.getContinueButtonIndex()) {
                HistoryBrowserDialogManager.getInstance().showHistory(dialog.getOsmIds());
            }
        }
    }

    /**
     * Dialog allowing to choose object id if no one is selected.
     * @since 6448
     */
    public static class HistoryObjectIDDialog extends OsmIdSelectionDialog {

        /**
         * Constructs a new {@code HistoryObjectIDDialog}.
         */
        public HistoryObjectIDDialog() {
            super(MainApplication.getMainFrame(), tr("Show history"), tr("Show history"), tr("Cancel"));
            setButtonIcons("dialogs/history", "cancel");
            init();
        }

        @Override
        public void setupDialog() {
            super.setupDialog();
            buttons.get(0).setEnabled(!NetworkManager.isOffline(OnlineResource.OSM_API));
        }
    }
}
