// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences;

/**
 * List class that read and save its content from the bookmark file.
 * @author imi
 */
public class BookmarkList extends JList {

    /**
     * Create a bookmark list as well as the Buttons add and remove.
     */
    public BookmarkList() {
        setModel(new DefaultListModel());
        load();
        setVisibleRowCount(7);
    }

    /**
     * Loads the bookmarks from file.
     */
    public void load() {
        DefaultListModel model = (DefaultListModel)getModel();
        model.removeAllElements();
        try {
            for (Preferences.Bookmark b : Main.pref.loadBookmarks()) {
                model.addElement(b);
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("<html>Could not read bookmarks from<br>''{0}''<br>Error was: {1}</html>",
                            Main.pref.getBookmarksFile(),
                            e.getMessage()
                    ),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    /**
     * Save all bookmarks to the preferences file
     */
    public void save() {
        try {
            Collection<Preferences.Bookmark> bookmarks = new LinkedList<Preferences.Bookmark>();
            for (Object o : ((DefaultListModel)getModel()).toArray()) {
                bookmarks.add((Preferences.Bookmark)o);
            }
            Main.pref.saveBookmarks(bookmarks);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("<html>Could not write bookmark.<br>{0}</html>", e.getMessage()),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
