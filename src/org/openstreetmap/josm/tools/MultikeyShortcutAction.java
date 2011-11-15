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
            if (index < 10)
                return (char)('0' + index);
            else
                return (char)('A' +  index - 10);
        }

        public String getDescription() {
            return description;
        }
    }

    void executeMultikeyAction(int index);
    void repeateLastMultikeyAction();
    List<MultikeyInfo> getMultikeyCombinations();
    MultikeyInfo getLastMultikeyAction();

}
