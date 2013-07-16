// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Collection;
import java.util.Collections;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.OpenFileAction.OpenFileTask;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Show list of recently opened files
 */
public class RecentlyOpenedFilesMenu extends JMenu {
    ClearAction clearAction;

    public RecentlyOpenedFilesMenu() {
        super(tr("Open Recent"));
        setToolTipText(tr("List of recently opened files"));
        setIcon(ImageProvider.get("openrecent.png"));
        putClientProperty("help", ht("/Action/OpenRecent"));

        // build dynamically
        addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                rebuild();
            }

            @Override
            public void menuDeselected(MenuEvent e) {
            }

            @Override
            public void menuCanceled(MenuEvent e) {
            }
        });
    }

    private void rebuild() {
        removeAll();
        Collection<String> fileHistory = Main.pref.getCollection("file-open.history");

        for (final String file : fileHistory) {
            add(new AbstractAction() {
                {
                    putValue(NAME, file);
                    putValue("help", ht("/Action/OpenRecent"));
                    putValue("toolbar", false);
                }
                @Override
                public void actionPerformed(ActionEvent e) {
                    File f = new File(file);
                    OpenFileTask task = new OpenFileTask(Collections.singletonList(f), null);
                    task.setRecordHistory(true);
                    Main.worker.submit(task);
                }
            });
        }
        add(new JSeparator());
        if (clearAction == null) {
            clearAction = new ClearAction();
        }
        JMenuItem clearItem = new JMenuItem(clearAction);
        clearItem.setEnabled(!fileHistory.isEmpty());
        add(clearItem);
    }

    private static class ClearAction extends AbstractAction {

        public ClearAction() {
            super(tr("Clear"));
            putValue(SHORT_DESCRIPTION, tr("Clear the list of recently opened files"));
            putValue("help", ht("/Action/OpenRecent"));
            putValue("toolbar", "recentlyopenedfiles/clear");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Main.pref.putCollection("file-open.history", null);
        }
    }
}
