// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Function;

import javax.swing.AbstractAction;

import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.history.History;
import org.openstreetmap.josm.data.osm.history.HistoryDataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Open a history browser with the history of this node
 */
class ShowHistoryAction extends AbstractAction {
    private transient PrimitiveId primitiveId;

    /**
     * Constructs a new {@code ShowHistoryAction}.
     */
    ShowHistoryAction() {
        putValue(NAME, tr("Show history"));
        putValue(SHORT_DESCRIPTION, tr("Open a history browser with the history of this node"));
        new ImageProvider("dialogs", "history").getResource().attachImageIcon(this, true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (isEnabled()) {
            run();
        }
    }

    public void setPrimitiveId(PrimitiveId pid) {
        this.primitiveId = pid;
        updateEnabledState();
    }

    public void run() {
        if (HistoryDataSet.getInstance().getHistory(primitiveId) == null) {
            MainApplication.worker.submit(new HistoryLoadTask().add(primitiveId));
        }
        MainApplication.worker.submit(() -> {
            final History h = HistoryDataSet.getInstance().getHistory(primitiveId);
            if (h == null)
                return;
            GuiHelper.runInEDT(() -> HistoryBrowserDialogManager.getInstance().show(h));
        });
    }

    public void updateEnabledState() {
        setEnabled(primitiveId != null && !primitiveId.isNew());
    }

    static class DoubleClickAdapter extends MouseAdapter {
        private final Function<MouseEvent, PrimitiveId> primitiveIdFunction;
        private final ShowHistoryAction showHistoryAction = new ShowHistoryAction();

        DoubleClickAdapter(Function<MouseEvent, PrimitiveId> primitiveIdFunction) {
            this.primitiveIdFunction = primitiveIdFunction;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() < 2)
                return;
            PrimitiveId pid = primitiveIdFunction.apply(e);
            if (pid == null || pid.isNew())
                return;
            showHistoryAction.setPrimitiveId(pid);
            showHistoryAction.run();
        }
    }
}
