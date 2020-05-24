// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.function.Function;

import org.openstreetmap.josm.actions.AbstractShowHistoryAction;
import org.openstreetmap.josm.data.osm.PrimitiveId;

/**
 * Open a history browser with the history of an object.
 */
class ShowHistoryAction extends AbstractShowHistoryAction {
    private transient PrimitiveId primitiveId;

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
            HistoryBrowserDialogManager.getInstance().showHistory(Collections.singleton(pid));
        }
    }
}
