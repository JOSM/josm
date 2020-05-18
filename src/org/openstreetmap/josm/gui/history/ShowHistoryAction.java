// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.function.Function;

import javax.swing.AbstractAction;

import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Open a history browser with the history of an object.
 */
class ShowHistoryAction extends AbstractAction {
    private transient PrimitiveId primitiveId;

    /**
     * Constructs a new {@code ShowHistoryAction}.
     */
    ShowHistoryAction() {
        putValue(NAME, tr("Show history"));
        putValue(SHORT_DESCRIPTION, tr("Display the history of the selected object."));
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
        HistoryBrowserDialogManager.getInstance().showHistory(Collections.singleton(primitiveId));
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
