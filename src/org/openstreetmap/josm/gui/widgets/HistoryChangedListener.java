// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.util.List;

public interface HistoryChangedListener {
    void historyChanged(List<String> history);
}
