// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;
import org.openstreetmap.josm.gui.mappaint.xml.XmlStyleSource;
import org.openstreetmap.josm.gui.preferences.SourceEntry;
import org.openstreetmap.josm.gui.preferences.MapPaintPreference.MapPaintPrefMigration;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.MirroredInputStream;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * This class manages the ElemStyles instance. The object you get with
 * getStyles() is read only, any manipulation happens via one of
 * the wrapper methods here. (readFromPreferences, moveStyles, ...)
 *
 * On change, mapPaintSylesUpdated() is fired for all listeners.
 */
public class MapPaintStyles {

    private static ElemStyles styles = new ElemStyles();

    public static ElemStyles getStyles()
    {
        return styles;
    }
    
    public static class IconReference {

        public String iconName;
        public StyleSource source;

        public IconReference(String iconName, StyleSource source) {
            this.iconName = iconName;
            this.source = source;
        }

        @Override
        public String toString() {
            return "IconReference{" + "iconName=" + iconName + " source=" + source.getDisplayString() + '}';
        }
    }

    public static ImageIcon getIcon(IconReference ref, boolean sanitize)
    {
        String namespace = ref.source.getPrefName();
        ImageIcon i = ImageProvider.getIfAvailable(getIconSourceDirs(ref.source), "mappaint."+namespace, null, ref.iconName, ref.source.zipIcons, sanitize);
        if(i == null)
        {
            System.out.println("Mappaint style \""+namespace+"\" icon \"" + ref.iconName + "\" not found.");
            return null;
        }
        return i;
    }

    /**
     * No icon with the given name was found, show a dummy icon instead
     * @return the icon misc/no_icon.png, in descending priority:
     *   - relative to source file
     *   - from user icon paths
     *   - josm's default icon
     *  can be null if the defaults are turned off by user
     */
    public static ImageIcon getNoIcon_Icon(StyleSource source, boolean sanitize) {
        return ImageProvider.getIfAvailable(getIconSourceDirs(source), "mappaint."+source.getPrefName(), null, "misc/no_icon.png", source.zipIcons, sanitize);
    }

    private static List<String> getIconSourceDirs(StyleSource source) {
        List<String> dirs = new LinkedList<String>();

        String sourceDir = source.getLocalSourceDir();
        if (sourceDir != null) {
            dirs.add(sourceDir);
        }

        Collection<String> prefIconDirs = Main.pref.getCollection("mappaint.icon.sources");
        for(String fileset : prefIconDirs)
        {
            String[] a;
            if(fileset.indexOf("=") >= 0) {
                a = fileset.split("=", 2);
            } else {
                a = new String[] {"", fileset};
            }

            /* non-prefixed path is generic path, always take it */
            if(a[0].length() == 0 || source.getPrefName().equals(a[0])) {
                dirs.add(a[1]);
            }
        }

        if (Main.pref.getBoolean("mappaint.icon.enable-defaults", true)) {
            /* don't prefix icon path, as it should be generic */
            dirs.add("resource://images/styles/standard/");
            dirs.add("resource://images/styles/");
        }
        
        return dirs;
    }

    public static void readFromPreferences() {
        styles.clear();

        Collection<? extends SourceEntry> sourceEntries = MapPaintPrefMigration.INSTANCE.get();

        for (SourceEntry entry : sourceEntries) {
            StyleSource source = fromSourceEntry(entry);
            if (source != null) {
                styles.add(source);
            }
        }
        for (StyleSource source : styles.getStyleSources()) {
            source.loadStyleSource();
        }
        
        fireMapPaintSylesUpdated();
    }

    private static StyleSource fromSourceEntry(SourceEntry entry) {
        MirroredInputStream in = null;
        try {
            in = new MirroredInputStream(entry.url);
            InputStream zip = in.getZipEntry("xml", "style");
            if (zip != null) {
                return new XmlStyleSource(entry);
            }
            zip = in.getZipEntry("mapcss", "style");
            if (zip != null) {
                return new MapCSSStyleSource(entry);
            }
            if (entry.url.toLowerCase().endsWith(".mapcss")) {
                return new MapCSSStyleSource(entry);
            } else {
                return new XmlStyleSource(entry);
            }
        } catch (IOException e) {
            System.err.println(tr("Warning: failed to load Mappaint styles from ''{0}''. Exception was: {1}", entry.url, e.toString()));
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
            }
        }
        return null;
    }

    /**
     * reload styles
     * preferences are the same, but the file source may have changed
     * @param sel the indices of styles to reload
     */
    public static void reloadStyles(final int... sel) {
        List<StyleSource> toReload = new ArrayList<StyleSource>();
        List<StyleSource> data = styles.getStyleSources();
        for (int i : sel) {
            toReload.add(data.get(i));
        }
        Main.worker.submit(new MapPaintStyleLoader(toReload));
    }

    public static class MapPaintStyleLoader extends PleaseWaitRunnable {
        private boolean canceled;
        private List<StyleSource> sources;

        public MapPaintStyleLoader(List<StyleSource> sources) {
            super(tr("Reloading style sources"));
            this.sources = sources;
        }

        @Override
        protected void cancel() {
            canceled = true;
        }

        @Override
        protected void finish() {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    fireMapPaintSylesUpdated();
                    styles.clearCached();
                    Main.map.mapView.preferenceChanged(null);
                    Main.map.mapView.repaint();
                }
            });
        }

        @Override
        protected void realRun() {
            ProgressMonitor monitor = getProgressMonitor();
            monitor.setTicksCount(sources.size());
            for (StyleSource s : sources) {
                if (canceled)
                    return;
                monitor.subTask(tr("loading style ''{0}''...", s.getDisplayString()));
                s.loadStyleSource();
                monitor.worked(1);
            }
        }
    }

    /**
     * Move position of entries in the current list of StyleSources
     * @param sele The indices of styles to be moved.
     * @param delta The number of lines it should move. positive int moves
     *      down and negative moves up.
     */
    public static void moveStyles(int[] sel, int delta) {
        if (!canMoveStyles(sel, delta))
            return;
        int[] selSorted = Arrays.copyOf(sel, sel.length);
        Arrays.sort(selSorted);
        List<StyleSource> data = new ArrayList<StyleSource>(styles.getStyleSources());
        for (int row: selSorted) {
            StyleSource t1 = data.get(row);
            StyleSource t2 = data.get(row + delta);
            data.set(row, t2);
            data.set(row + delta, t1);
        }
        styles.setStyleSources(data);
        MapPaintPrefMigration.INSTANCE.put(data);
        fireMapPaintSylesUpdated();
        styles.clearCached();
        Main.map.mapView.repaint();
    }

    public static boolean canMoveStyles(int[] sel, int i) {
        if (sel.length == 0)
            return false;
        int[] selSorted = Arrays.copyOf(sel, sel.length);
        Arrays.sort(selSorted);

        if (i < 0) { // Up
            return selSorted[0] >= -i;
        } else
        if (i > 0) { // Down
            return selSorted[selSorted.length-1] <= styles.getStyleSources().size() - 1 - i;
        } else
            return true;
    }

    public static void toggleStyleActive(int... sel) {
        List<StyleSource> data = styles.getStyleSources();
        for (int p : sel) {
            StyleSource s = data.get(p);
            s.active = !s.active;
        }
        MapPaintPrefMigration.INSTANCE.put(data);
        if (sel.length == 1) {
            fireMapPaintStyleEntryUpdated(sel[0]);
        } else {
            fireMapPaintSylesUpdated();
        }
        styles.clearCached();
        Main.map.mapView.repaint();
    }

    public static void addStyle(SourceEntry entry) {
        StyleSource source = fromSourceEntry(entry);
        if (source != null) {
            styles.add(source);
            source.loadStyleSource();
            MapPaintPrefMigration.INSTANCE.put(styles.getStyleSources());
            fireMapPaintSylesUpdated();
            styles.clearCached();
            Main.map.mapView.repaint();
        }
    }

    /***********************************
     * MapPaintSylesUpdateListener & related code
     *  (get informed when the list of MapPaint StyleSources changes)
     */

    public interface MapPaintSylesUpdateListener {
        public void mapPaintStylesUpdated();
        public void mapPaintStyleEntryUpdated(int idx);
    }

    protected static final CopyOnWriteArrayList<MapPaintSylesUpdateListener> listeners
            = new CopyOnWriteArrayList<MapPaintSylesUpdateListener>();

    public static void addMapPaintSylesUpdateListener(MapPaintSylesUpdateListener listener) {
        if (listener != null) {
            listeners.addIfAbsent(listener);
        }
    }

    public static void removeMapPaintSylesUpdateListener(MapPaintSylesUpdateListener listener) {
        listeners.remove(listener);
    }

    public static void fireMapPaintSylesUpdated() {
        for (MapPaintSylesUpdateListener l : listeners) {
            l.mapPaintStylesUpdated();
        }
    }

    public static void fireMapPaintStyleEntryUpdated(int idx) {
        for (MapPaintSylesUpdateListener l : listeners) {
            l.mapPaintStyleEntryUpdated(idx);
        }
    }
}
