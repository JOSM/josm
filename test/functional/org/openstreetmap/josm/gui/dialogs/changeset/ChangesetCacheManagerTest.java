// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import org.junit.Ignore;

import javax.swing.JFrame;

@Ignore
public class ChangesetCacheManagerTest extends JFrame {

    private ChangesetCacheManager manager;

    public void start() {
        manager = new ChangesetCacheManager();
        manager.setVisible(true);
    }

    static public void main(String args[]) {
        new ChangesetCacheManagerTest().start();
    }
}
