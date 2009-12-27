// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import javax.swing.JFrame;

public class ChangesetCacheManagerTest extends JFrame {

    private ChangesetCacheManager manager;

    public ChangesetCacheManagerTest() {
    }

    public void start() {
        manager = new ChangesetCacheManager();
        manager.setVisible(true);
    }

    static public void main(String args[]) {
        new ChangesetCacheManagerTest().start();
    }
}
