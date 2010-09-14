// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * List class that read and save its content from the bookmark file.
 * @author imi
 */
public class BookmarkList extends JList {

    /**
     * Class holding one bookmarkentry.
     * @author imi
     */
    public static class Bookmark implements Comparable<Bookmark> {
        private String name;
        private Bounds area;

        public Bookmark(Collection<String> list) throws NumberFormatException, IllegalArgumentException {
            ArrayList<String> array = new ArrayList<String>(list);
            if(array.size() < 5)
                throw new IllegalArgumentException(tr("Wrong number of arguments for bookmark"));
            name = array.get(0);
            area = new Bounds(Double.parseDouble(array.get(1)), Double.parseDouble(array.get(2)),
                              Double.parseDouble(array.get(3)), Double.parseDouble(array.get(4)));
        }

        public Bookmark() {
            area = null;
            name = null;
        }

        public Bookmark(Bounds area) {
            this.area = area;
        }

        @Override public String toString() {
            return name;
        }

        public int compareTo(Bookmark b) {
            return name.toLowerCase().compareTo(b.name.toLowerCase());
        }

        public Bounds getArea() {
            return area;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setArea(Bounds area) {
            this.area = area;
        }
    }

    /**
     * Create a bookmark list as well as the Buttons add and remove.
     */
    public BookmarkList() {
        setModel(new DefaultListModel());
        load();
        setVisibleRowCount(7);
        setCellRenderer(new BookmarkCellRenderer());
    }

    /**
     * Loads the bookmarks from file.
     */
    public void load() {
        DefaultListModel model = (DefaultListModel)getModel();
        model.removeAllElements();
        Collection<Collection<String>> args = Main.pref.getArray("bookmarks", null);
        if(args != null) {
            LinkedList<Bookmark> bookmarks = new LinkedList<Bookmark>();
            for(Collection<String> entry : args) {
                try {
                    bookmarks.add(new Bookmark(entry));
                }
                catch(Exception e) {
                    System.err.println(tr("Error reading bookmark entry: %s", e.getMessage()));
                }
            }
            Collections.sort(bookmarks);
            for (Bookmark b : bookmarks) {
                model.addElement(b);
            }
        }
        else if(!Main.applet) { /* FIXME: remove else clause after spring 2011 */
            File bookmarkFile = new File(Main.pref.getPreferencesDir(),"bookmarks");
            try {
                LinkedList<Bookmark> bookmarks = new LinkedList<Bookmark>();
                if (bookmarkFile.exists()) {
                    System.out.println("Try loading obsolete bookmarks file");
                    BufferedReader in = new BufferedReader(new InputStreamReader(
                            new FileInputStream(bookmarkFile), "utf-8"));

                    for (String line = in.readLine(); line != null; line = in.readLine()) {
                        Matcher m = Pattern.compile("^(.+)[,\u001e](-?\\d+.\\d+)[,\u001e](-?\\d+.\\d+)[,\u001e](-?\\d+.\\d+)[,\u001e](-?\\d+.\\d+)$").matcher(line);
                        if (!m.matches() || m.groupCount() != 5) {
                            System.err.println(tr("Error: Unexpected line ''{0}'' in bookmark file ''{1}''",line, bookmarkFile.toString()));
                            continue;
                        }
                        Bookmark b = new Bookmark();
                        b.setName(m.group(1));
                        double[] values= new double[4];
                        for (int i = 0; i < 4; ++i) {
                            try {
                                values[i] = Double.parseDouble(m.group(i+2));
                            } catch(NumberFormatException e) {
                                System.err.println(tr("Error: Illegal double value ''{0}'' on line ''{1}'' in bookmark file ''{2}''",m.group(i+2),line, bookmarkFile.toString()));
                                continue;
                            }
                        }
                        b.setArea(new Bounds(values));
                        bookmarks.add(b);
                    }
                    in.close();
                    Collections.sort(bookmarks);
                    for (Bookmark b : bookmarks) {
                        model.addElement(b);
                    }
                    save();
                    System.out.println("Removing obsolete bookmarks file");
                    bookmarkFile.delete();
                }
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("<html>Could not read bookmarks from<br>''{0}''<br>Error was: {1}</html>",
                                bookmarkFile.toString(),
                                e.getMessage()
                        ),
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    /**
     * Save all bookmarks to the preferences file
     */
    public void save() {
        LinkedList<Collection<String>> coll = new LinkedList<Collection<String>>();
        for (Object o : ((DefaultListModel)getModel()).toArray()) {
            String[] array = new String[5];
            Bookmark b = (Bookmark)o;
            array[0] = b.getName();
            Bounds area = b.getArea();
            array[1] = String.valueOf(area.getMin().lat());
            array[2] = String.valueOf(area.getMin().lon());
            array[3] = String.valueOf(area.getMax().lat());
            array[4] = String.valueOf(area.getMax().lon());
            coll.add(Arrays.asList(array));
        }
        Main.pref.putArray("bookmarks", coll);
    }

    static class BookmarkCellRenderer extends JLabel implements ListCellRenderer {

        private ImageIcon icon;

        public BookmarkCellRenderer() {
            setOpaque(true);
            icon = ImageProvider.get("dialogs", "bookmark");
            setIcon(icon);
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
            StringBuffer sb = new StringBuffer();
            sb.append("<html>min[latitude,longitude]=<strong>[")
            .append(area.getMin().lat()).append(",").append(area.getMin().lon()).append("]</strong>")
            .append("<br>")
            .append("max[latitude,longitude]=<strong>[")
            .append(area.getMax().lat()).append(",").append(area.getMax().lon()).append("]</strong>")
            .append("</html>");
            return sb.toString();

        }
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                boolean cellHasFocus) {

            Bookmark b = (Bookmark) value;
            renderColor(isSelected);
            setText(b.getName());
            setToolTipText(buildToolTipText(b));
            return this;
        }
    }
}
