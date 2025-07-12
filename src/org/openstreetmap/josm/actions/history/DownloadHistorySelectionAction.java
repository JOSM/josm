// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.history.HistoryLoadTask;
import org.openstreetmap.josm.io.NetworkManager;
import org.openstreetmap.josm.io.OnlineResource;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;

/**
 * This action downloads and caches the history for selected primitives.
 * @since 1670
 */
public class DownloadHistorySelectionAction extends JosmAction {

    /**
     * Constructs a new {@code UpdateSelectionAction}.
     */
    public DownloadHistorySelectionAction() {
        super(tr("Download history for selection"), "download",
                tr("Downloads and caches the history for currently selected objects from the server"),
                Shortcut.registerShortcut("file:downloadhistoryselection",
                        tr("File: {0}", tr("Download in current view")), KeyEvent.CHAR_UNDEFINED, Shortcut.NONE),
                true, "downloadhistoryselection", true);
    }

    @Override
    protected void updateEnabledState() {
        updateEnabledStateOnCurrentSelection();
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        if (Utils.isEmpty(selection)) {
            setEnabled(false);
        } else {
            setEnabled(!NetworkManager.isOffline(OnlineResource.OSM_API) &&
                       selection.stream().anyMatch(p -> !p.isNew()));
        }
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        if (!isEnabled())
            return;

        HistoryLoadTask task = new HistoryLoadTask();
        task.addOsmPrimitives(getData());
        MainApplication.worker.submit(task);
    }

    /**
     * Returns the data on which this action operates. Override if needed.
     * @return the data on which this action operates
     */
    public Collection<OsmPrimitive> getData() {
        return getLayerManager().getActiveDataSet().getAllSelected();
    }
}
