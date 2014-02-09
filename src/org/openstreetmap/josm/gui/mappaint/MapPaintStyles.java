// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.mappaint.StyleCache.StyleList;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;
import org.openstreetmap.josm.gui.mappaint.xml.XmlStyleSource;
import org.openstreetmap.josm.gui.preferences.SourceEntry;
import org.openstreetmap.josm.gui.preferences.map.MapPaintPreference.MapPaintPrefHelper;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.MirroredInputStream;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Utils;

/**
 * This class manages the ElemStyles instance. The object you get with
 * getStyles() is read only, any manipulation happens via one of
 * the wrapper methods here. (readFromPreferences, moveStyles, ...)
 *
 * On change, mapPaintSylesUpdated() is fired for all listeners.
 */
public final class MapPaintStyles {

    private static ElemStyles styles = new ElemStyles();

    /**
     * Returns the {@link ElemStyles} instance.
     * @return the {@code ElemStyles} instance
     */
    public static ElemStyles getStyles() {
        return styles;
    }

    private MapPaintStyles() {
        // Hide default constructor for utils classes
    }

    /**
     * Value holder for a reference to a tag name. A style instruction
     * <pre>
     *    text: a_tag_name;
     * </pre>
     * results in a tag reference for the tag <tt>a_tag_name</tt> in the
     * style cascade.
     */
    public static class TagKeyReference {
        public final String key;
        public TagKeyReference(String key) {
            this.key = key;
        }

        @Override
        public String toString() {
            return "TagKeyReference{" + "key='" + key + "'}";
        }
    }

    /**
     * IconReference is used to remember the associated style source for
     * each icon URL.
     * This is necessary because image URLs can be paths relative
     * to the source file and we have cascading of properties from different
     * source files.
     */
    public static class IconReference {

        public final String iconName;
        public final StyleSource source;

        public IconReference(String iconName, StyleSource source) {
            this.iconName = iconName;
            this.source = source;
        }

        @Override
        public String toString() {
            return "IconReference{" + "iconName='" + iconName + "' source='" + source.getDisplayString() + "'}";
        }
    }

    public static ImageIcon getIcon(IconReference ref, int width, int height) {
        final String namespace = ref.source.getPrefName();
        ImageIcon i = new ImageProvider(ref.iconName)
                .setDirs(getIconSourceDirs(ref.source))
                .setId("mappaint."+namespace)
                .setArchive(ref.source.zipIcons)
                .setInArchiveDir(ref.source.getZipEntryDirName())
                .setWidth(width)
                .setHeight(height)
                .setOptional(true).get();
        if (i == null) {
            Main.warn("Mappaint style \""+namespace+"\" ("+ref.source.getDisplayString()+") icon \"" + ref.iconName + "\" not found.");
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
    public static ImageIcon getNoIcon_Icon(StyleSource source) {
        return new ImageProvider("misc/no_icon.png")
                .setDirs(getIconSourceDirs(source))
                .setId("mappaint."+source.getPrefName())
                .setArchive(source.zipIcons)
                .setInArchiveDir(source.getZipEntryDirName())
                .setOptional(true).get();
    }

    public static ImageIcon getNodeIcon(Tag tag) {
        return getNodeIcon(tag, true);
    }

    public static ImageIcon getNodeIcon(Tag tag, boolean includeDeprecatedIcon) {
        if (tag != null) {
            Node virtualNode = new Node();
            virtualNode.put(tag.getKey(), tag.getValue());
            StyleList styleList = getStyles().generateStyles(virtualNode, 0.5, null, false).a;
            if (styleList != null) {
                for (ElemStyle style : styleList) {
                    if (style instanceof NodeElemStyle) {
                        MapImage mapImage = ((NodeElemStyle) style).mapImage;
                        if (mapImage != null) {
                            if (includeDeprecatedIcon || mapImage.name == null || !mapImage.name.equals("misc/deprecated.png")) {
                                return new ImageIcon(mapImage.getDisplayedNodeIcon(false));
                            } else {
                                return null; // Deprecated icon found but not wanted
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public static List<String> getIconSourceDirs(StyleSource source) {
        List<String> dirs = new LinkedList<String>();

        String sourceDir = source.getLocalSourceDir();
        if (sourceDir != null) {
            dirs.add(sourceDir);
        }

        Collection<String> prefIconDirs = Main.pref.getCollection("mappaint.icon.sources");
        for(String fileset : prefIconDirs)
        {
            String[] a;
            if(fileset.indexOf('=') >= 0) {
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

        Collection<? extends SourceEntry> sourceEntries = MapPaintPrefHelper.INSTANCE.get();

        for (SourceEntry entry : sourceEntries) {
            StyleSource source = fromSourceEntry(entry);
            if (source != null) {
                styles.add(source);
            }
        }
        for (StyleSource source : styles.getStyleSources()) {
            source.loadStyleSource();
            if (Main.pref.getBoolean("mappaint.auto_reload_local_styles", true)) {
                if (source.isLocal()) {
                    File f = new File(source.url);
                    source.setLastMTime(f.lastModified());
                }
            }
        }
        fireMapPaintSylesUpdated();
    }

    private static StyleSource fromSourceEntry(SourceEntry entry) {
        MirroredInputStream in = null;
        try {
            in = new MirroredInputStream(entry.url);
            String zipEntryPath = in.findZipEntryPath("mapcss", "style");
            if (zipEntryPath != null) {
                entry.isZip = true;
                entry.zipEntryPath = zipEntryPath;
                return new MapCSSStyleSource(entry);
            }
            zipEntryPath = in.findZipEntryPath("xml", "style");
            if (zipEntryPath != null)
                return new XmlStyleSource(entry);
            if (entry.url.toLowerCase().endsWith(".mapcss"))
                return new MapCSSStyleSource(entry);
            if (entry.url.toLowerCase().endsWith(".xml"))
                return new XmlStyleSource(entry);
            else {
                InputStreamReader reader = new InputStreamReader(in);
                try {
                    WHILE: while (true) {
                        int c = reader.read();
                        switch (c) {
                            case -1:
                                break WHILE;
                            case ' ':
                            case '\t':
                            case '\n':
                            case '\r':
                                continue;
                            case '<':
                                return new XmlStyleSource(entry);
                            default:
                                return new MapCSSStyleSource(entry);
                        }
                    }
                } finally {
                    reader.close();
                }
                Main.warn("Could not detect style type. Using default (xml).");
                return new XmlStyleSource(entry);
            }
        } catch (IOException e) {
            Main.warn(tr("Failed to load Mappaint styles from ''{0}''. Exception was: {1}", entry.url, e.toString()));
            Main.error(e);
        } finally {
            Utils.close(in);
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
     * @param sel The indices of styles to be moved.
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
        MapPaintPrefHelper.INSTANCE.put(data);
        fireMapPaintSylesUpdated();
        styles.clearCached();
        Main.map.mapView.repaint();
    }

    public static boolean canMoveStyles(int[] sel, int i) {
        if (sel.length == 0)
            return false;
        int[] selSorted = Arrays.copyOf(sel, sel.length);
        Arrays.sort(selSorted);

        if (i < 0) // Up
            return selSorted[0] >= -i;
        else if (i > 0) // Down
            return selSorted[selSorted.length-1] <= styles.getStyleSources().size() - 1 - i;
        else
            return true;
    }

    public static void toggleStyleActive(int... sel) {
        List<StyleSource> data = styles.getStyleSources();
        for (int p : sel) {
            StyleSource s = data.get(p);
            s.active = !s.active;
        }
        MapPaintPrefHelper.INSTANCE.put(data);
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
            MapPaintPrefHelper.INSTANCE.put(styles.getStyleSources());
            fireMapPaintSylesUpdated();
            styles.clearCached();
            Main.map.mapView.repaint();
        }
    }

    /***********************************
     * MapPaintSylesUpdateListener &amp; related code
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
