// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import javax.swing.JFrame;

import org.junit.Ignore;

@Ignore
public class ChangesetCacheManagerTest extends JFrame {

    private ChangesetCacheManager manager;

    public void start() {
        manager = new ChangesetCacheManager();
        manager.setVisible(true);
    }

    public static void main(String[] args) {
        new ChangesetCacheManagerTest().start();
    }
}
