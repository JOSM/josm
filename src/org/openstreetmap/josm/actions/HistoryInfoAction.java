// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.OsmIdSelectionDialog;
import org.openstreetmap.josm.gui.history.HistoryBrowserDialogManager;
import org.openstreetmap.josm.io.OnlineResource;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Display history information about OSM ways, nodes, or relations.
 * @since 968
 */
public class HistoryInfoAction extends JosmAction {

    /**
     * Constructs a new {@code HistoryInfoAction}.
     */
    public HistoryInfoAction() {
        super(tr("History"), "dialogs/history",
                tr("Display history information about OSM ways, nodes, or relations."),
                Shortcut.registerShortcut("core:historyinfo",
                        tr("History"), KeyEvent.VK_H, Shortcut.CTRL), false);
        putValue("help", ht("/Action/ObjectHistory"));
        putValue("toolbar", "action/historyinfo");
        MainApplication.getToolbar().register(this);
        setEnabled(true);
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        DataSet set = getLayerManager().getActiveDataSet();
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
            super(Main.parent, tr("Show history"), tr("Show history"), tr("Cancel"));
            setButtonIcons("dialogs/history", "cancel");
            init();
        }

        @Override
        public void setupDialog() {
            super.setupDialog();
            buttons.get(0).setEnabled(!Main.isOffline(OnlineResource.OSM_API));
        }
    }
}
