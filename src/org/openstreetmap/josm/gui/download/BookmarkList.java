// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.download;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * List class that read and save its content from the bookmark file.
 * @since 6340
 */
public class BookmarkList extends JList<BookmarkList.Bookmark> {

    /**
     * Class holding one bookmarkentry.
     */
    public static class Bookmark implements Comparable<Bookmark> {
        private String name;
        private Bounds area;

        /**
         * Constructs a new {@code Bookmark} with the given contents.
         * @param list Bookmark contents as a list of 5 elements.
         * First item is the name, then come bounds arguments (minlat, minlon, maxlat, maxlon)
         * @throws NumberFormatException if the bounds arguments are not numbers
         * @throws IllegalArgumentException if list contain less than 5 elements
         */
        public Bookmark(Collection<String> list) {
            List<String> array = new ArrayList<>(list);
            if (array.size() < 5)
                throw new IllegalArgumentException(tr("Wrong number of arguments for bookmark"));
            name = array.get(0);
            area = new Bounds(Double.parseDouble(array.get(1)), Double.parseDouble(array.get(2)),
                              Double.parseDouble(array.get(3)), Double.parseDouble(array.get(4)));
        }

        /**
         * Constructs a new empty {@code Bookmark}.
         */
        public Bookmark() {
            area = null;
            name = null;
        }

        /**
         * Constructs a new unamed {@code Bookmark} for the given area.
         * @param area The bookmark area
         */
        public Bookmark(Bounds area) {
            this.area = area;
        }

        @Override public String toString() {
            return name;
        }

        @Override
        public int compareTo(Bookmark b) {
            return name.toLowerCase(Locale.ENGLISH).compareTo(b.name.toLowerCase(Locale.ENGLISH));
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((area == null) ? 0 : area.hashCode());
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Bookmark other = (Bookmark) obj;
            if (area == null) {
                if (other.area != null)
                    return false;
            } else if (!area.equals(other.area))
                return false;
            if (name == null) {
                if (other.name != null)
                    return false;
            } else if (!name.equals(other.name))
                return false;
            return true;
        }

        /**
         * Returns the bookmark area
         * @return The bookmark area
         */
        public Bounds getArea() {
            return area;
        }

        /**
         * Returns the bookmark name
         * @return The bookmark name
         */
        public String getName() {
            return name;
        }

        /**
         * Sets the bookmark name
         * @param name The bookmark name
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Sets the bookmark area
         * @param area The bookmark area
         */
        public void setArea(Bounds area) {
            this.area = area;
        }
    }

    /**
     * Creates a bookmark list as well as the Buttons add and remove.
     */
    public BookmarkList() {
        setModel(new DefaultListModel<Bookmark>());
        load();
        setVisibleRowCount(7);
        setCellRenderer(new BookmarkCellRenderer());
    }

    /**
     * Loads the bookmarks from file.
     */
    public final void load() {
        DefaultListModel<Bookmark> model = (DefaultListModel<Bookmark>) getModel();
        model.removeAllElements();
        Collection<Collection<String>> args = Main.pref.getArray("bookmarks", null);
        if (args != null) {
            List<Bookmark> bookmarks = new LinkedList<>();
            for (Collection<String> entry : args) {
                try {
                    bookmarks.add(new Bookmark(entry));
                } catch (Exception e) {
                    Main.error(tr("Error reading bookmark entry: %s", e.getMessage()));
                }
            }
            Collections.sort(bookmarks);
            for (Bookmark b : bookmarks) {
                model.addElement(b);
            }
        }
    }

    /**
     * Saves all bookmarks to the preferences file
     */
    public final void save() {
        List<Collection<String>> coll = new LinkedList<>();
        for (Object o : ((DefaultListModel<Bookmark>) getModel()).toArray()) {
            String[] array = new String[5];
            Bookmark b = (Bookmark) o;
            array[0] = b.getName();
            Bounds area = b.getArea();
            array[1] = String.valueOf(area.getMinLat());
            array[2] = String.valueOf(area.getMinLon());
            array[3] = String.valueOf(area.getMaxLat());
            array[4] = String.valueOf(area.getMaxLon());
            coll.add(Arrays.asList(array));
        }
        Main.pref.putArray("bookmarks", coll);
    }

    static class BookmarkCellRenderer extends JLabel implements ListCellRenderer<BookmarkList.Bookmark> {

        /**
         * Constructs a new {@code BookmarkCellRenderer}.
         */
        BookmarkCellRenderer() {
            setOpaque(true);
            setIcon(ImageProvider.get("dialogs", "bookmark"));
        }

        protected void renderColor(boolean selected) {
            if (selected) {
                setBackground(UIManager.getColor("List.selectionBackground"));
                setForeground(UIManager.getColor("List.selectionForeground"));
            } else {
                setBackground(UIManager.getColor("List.background"));
                setForeground(UIManager.getColor("List.foreground"));
            }
        }

        protected String buildToolTipText(Bookmark b) {
            Bounds area = b.getArea();
            StringBuilder sb = new StringBuilder(128);
            sb.append("<html>min[latitude,longitude]=<strong>[")
              .append(area.getMinLat()).append(',').append(area.getMinLon()).append("]</strong>"+
                      "<br>max[latitude,longitude]=<strong>[")
              .append(area.getMaxLat()).append(',').append(area.getMaxLon()).append("]</strong>"+
                      "</html>");
            return sb.toString();
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Bookmark> list, Bookmark value, int index, boolean isSelected,
                boolean cellHasFocus) {
            renderColor(isSelected);
            setText(value.getName());
            setToolTipText(buildToolTipText(value));
            return this;
        }
    }
}
