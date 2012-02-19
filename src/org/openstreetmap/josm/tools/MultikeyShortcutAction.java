// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.util.List;

import javax.swing.Action;

public interface MultikeyShortcutAction extends Action {

    public static class MultikeyInfo {
        private final int index;
        private final String description;

        public MultikeyInfo(int index, String description) {
            this.index = index;
            this.description = description;
        }

        public int getIndex() {
            return index;
        }

        public char getShortcut() {
            if (index < 9)
                return (char)('1' + index);
            else if (index == 9)
                return '0';
            else
                return (char)('A' +  index - 10);
        }

        public String getDescription() {
            return description;
        }
    }

    public Shortcut getMultikeyShortcut();

    void executeMultikeyAction(int index, boolean repeatLastAction);
    List<MultikeyInfo> getMultikeyCombinations();
    MultikeyInfo getLastMultikeyAction();

}
