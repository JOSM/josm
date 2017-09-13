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
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Show list of recently opened files
 */
public class RecentlyOpenedFilesMenu extends JMenu {
    private ClearAction clearAction;

    /**
     * Constructs a new {@code RecentlyOpenedFilesMenu}.
     */
    public RecentlyOpenedFilesMenu() {
        super(tr("Open Recent"));
        setToolTipText(tr("List of recently opened files"));
        setIcon(ImageProvider.get("openrecent", ImageProvider.ImageSizes.MENU));
        putClientProperty("help", ht("/Action/OpenRecent"));

        // build dynamically
        addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                rebuild();
            }

            @Override
            public void menuDeselected(MenuEvent e) {
                // Do nothing
            }

            @Override
            public void menuCanceled(MenuEvent e) {
                // Do nothing
            }
        });
    }

    private void rebuild() {
        removeAll();
        Collection<String> fileHistory = Main.pref.getList("file-open.history");

        for (final String file : fileHistory) {
            add(new OpenRecentAction(file));
        }
        add(new JSeparator());
        if (clearAction == null) {
            clearAction = new ClearAction();
        }
        JMenuItem clearItem = new JMenuItem(clearAction);
        clearItem.setEnabled(!fileHistory.isEmpty());
        add(clearItem);
    }

    static final class OpenRecentAction extends AbstractAction {
        private final String file;

        OpenRecentAction(String file) {
            this.file = file;
            putValue(NAME, file);
            putValue("help", ht("/Action/OpenRecent"));
            putValue("toolbar", Boolean.FALSE);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            OpenFileTask task = new OpenFileTask(Collections.singletonList(new File(file)), null);
            task.setRecordHistory(true);
            MainApplication.worker.submit(task);
        }
    }

    private static class ClearAction extends AbstractAction {

        ClearAction() {
            super(tr("Clear"));
            putValue(SHORT_DESCRIPTION, tr("Clear the list of recently opened files"));
            putValue("help", ht("/Action/OpenRecent"));
            putValue("toolbar", "recentlyopenedfiles/clear");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Main.pref.putList("file-open.history", null);
        }
    }
}
