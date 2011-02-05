// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
    private static Collection<String> iconDirs;

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

    public static ImageIcon getIcon(IconReference ref)
    {
        String styleName = ref.source.getPrefName();
        List<String> dirs = new LinkedList<String>();
        for(String fileset : iconDirs)
        {
            String[] a;
            if(fileset.indexOf("=") >= 0) {
                a = fileset.split("=", 2);
            } else {
                a = new String[] {"", fileset};
            }

            /* non-prefixed path is generic path, always take it */
            if(a[0].length() == 0 || styleName.equals(a[0])) {
                dirs.add(a[1]);
            }
        }
        ImageIcon i = ImageProvider.getIfAvailable(dirs, "mappaint."+styleName, null, ref.iconName, ref.source.zipIcons);
        if(i == null)
        {
            System.out.println("Mappaint style \""+styleName+"\" icon \"" + ref.iconName + "\" not found.");
            i = ImageProvider.getIfAvailable(dirs, "mappaint."+styleName, null, "misc/no_icon.png");
        }
        return i;
    }

    public static void readFromPreferences() {
        styles.clear();
        iconDirs = Main.pref.getCollection("mappaint.icon.sources", Collections.<String>emptySet());
        if(Main.pref.getBoolean("mappaint.icon.enable-defaults", true))
        {
            LinkedList<String> f = new LinkedList<String>(iconDirs);
            /* don't prefix icon path, as it should be generic */
            f.add("resource://images/styles/standard/");
            f.add("resource://images/styles/");
            iconDirs = f;
        }

        Collection<? extends SourceEntry> sourceEntries = MapPaintPrefMigration.INSTANCE.get();

        for (SourceEntry entry : sourceEntries) {
            StyleSource style = null;
            try {
                MirroredInputStream in = new MirroredInputStream(entry.url);
                InputStream zip = in.getZipEntry("xml","style");
                if (zip != null) {
                    style = new XmlStyleSource(entry);
                    styles.add(style);
                    continue;
                }
                zip = in.getZipEntry("mapcss","style");
                if (zip != null) {
                    style = new MapCSSStyleSource(entry);
                    styles.add(style);
                    continue;
                }
                if (entry.url.toLowerCase().endsWith(".mapcss")) {
                    style = new MapCSSStyleSource(entry);
                } else {
                    style = new XmlStyleSource(entry);
                }
            } catch(IOException e) {
                System.err.println(tr("Warning: failed to load Mappaint styles from ''{0}''. Exception was: {1}", entry.url, e.toString()));
                e.printStackTrace();
                if (style != null) {
                    style.hasError = true;
                }
            }
            if (style != null) {
                styles.add(style);
            }
        }
        for (StyleSource s : styles.getStyleSources()) {
            s.loadStyleSource();
        }
        fireMapPaintSylesUpdated();
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
