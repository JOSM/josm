// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.preferences.sources.MapPaintPrefHelper;
import org.openstreetmap.josm.data.preferences.sources.SourceEntry;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.io.FileWatcher;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ListenerList;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Stopwatch;
import org.openstreetmap.josm.tools.Utils;

/**
 * This class manages the list of available map paint styles and gives access to
 * the ElemStyles singleton.
 *
 * On change, {@link MapPaintSylesUpdateListener#mapPaintStylesUpdated()} is fired
 * for all listeners.
 */
public final class MapPaintStyles {

    private static final Collection<String> DEPRECATED_IMAGE_NAMES = Arrays.asList(
            "presets/misc/deprecated.svg",
            "misc/deprecated.png");

    private static final ListenerList<MapPaintSylesUpdateListener> listeners = ListenerList.createUnchecked();

    static {
        listeners.addListener(new MapPaintSylesUpdateListener() {
            @Override
            public void mapPaintStylesUpdated() {
                SwingUtilities.invokeLater(styles::clearCached);
            }

            @Override
            public void mapPaintStyleEntryUpdated(int index) {
                mapPaintStylesUpdated();
            }
        });
    }

    private static ElemStyles styles = new ElemStyles();

    /**
     * Returns the {@link ElemStyles} singleton instance.
     *
     * The returned object is read only, any manipulation happens via one of
     * the other wrapper methods in this class. ({@link #readFromPreferences},
     * {@link #moveStyles}, ...)
     * @return the {@code ElemStyles} singleton instance
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
     * results in a tag reference for the tag <code>a_tag_name</code> in the
     * style cascade.
     */
    public static class TagKeyReference {
        /**
         * The tag name
         */
        public final String key;

        /**
         * Create a new {@link TagKeyReference}
         * @param key The tag name
         */
        public TagKeyReference(String key) {
            this.key = key.intern();
        }

        @Override
        public String toString() {
            return "TagKeyReference{" + "key='" + key + "'}";
        }
    }

    /**
     * IconReference is used to remember the associated style source for each icon URL.
     * This is necessary because image URLs can be paths relative
     * to the source file and we have cascading of properties from different source files.
     */
    public static class IconReference {

        /**
         * The name of the icon
         */
        public final String iconName;
        /**
         * The style source this reference occurred in
         */
        public final StyleSource source;

        /**
         * Create a new {@link IconReference}
         * @param iconName The icon name
         * @param source The current style source
         */
        public IconReference(String iconName, StyleSource source) {
            this.iconName = iconName;
            this.source = source;
        }

        @Override
        public String toString() {
            return "IconReference{" + "iconName='" + iconName + "' source='" + source.getDisplayString() + "'}";
        }

        /**
         * Determines whether this icon represents a deprecated icon
         * @return whether this icon represents a deprecated icon
         * @since 10927
         */
        public boolean isDeprecatedIcon() {
            return DEPRECATED_IMAGE_NAMES.contains(iconName);
        }
    }

    /**
     * Image provider for icon. Note that this is a provider only. A @link{ImageProvider#get()} call may still fail!
     *
     * @param ref reference to the requested icon
     * @param test if <code>true</code> than the icon is request is tested
     * @return image provider for icon (can be <code>null</code> when <code>test</code> is <code>true</code>).
     * @see #getIcon(IconReference, int,int)
     * @since 8097
     */
    public static ImageProvider getIconProvider(IconReference ref, boolean test) {
        final String namespace = ref.source.getPrefName();
        ImageProvider i = new ImageProvider(ref.iconName)
                .setDirs(getIconSourceDirs(ref.source))
                .setId("mappaint."+namespace)
                .setArchive(ref.source.zipIcons)
                .setInArchiveDir(ref.source.getZipEntryDirName())
                .setOptional(true);
        if (test && i.get() == null) {
            String msg = "Mappaint style \""+namespace+"\" ("+ref.source.getDisplayString()+") icon \"" + ref.iconName + "\" not found.";
            ref.source.logWarning(msg);
            Logging.warn(msg);
            return null;
        }
        return i;
    }

    /**
     * Return scaled icon.
     *
     * @param ref reference to the requested icon
     * @param width icon width or -1 for autoscale
     * @param height icon height or -1 for autoscale
     * @return image icon or <code>null</code>.
     * @see #getIconProvider(IconReference, boolean)
     */
    public static ImageIcon getIcon(IconReference ref, int width, int height) {
        final String namespace = ref.source.getPrefName();
        ImageIcon i = getIconProvider(ref, false).setSize(width, height).get();
        if (i == null) {
            Logging.warn("Mappaint style \""+namespace+"\" ("+ref.source.getDisplayString()+") icon \"" + ref.iconName + "\" not found.");
            return null;
        }
        return i;
    }

    /**
     * No icon with the given name was found, show a dummy icon instead
     * @param source style source
     * @return the icon misc/no_icon.png, in descending priority:
     *   - relative to source file
     *   - from user icon paths
     *   - josm's default icon
     *  can be null if the defaults are turned off by user
     */
    public static ImageIcon getNoIconIcon(StyleSource source) {
        return new ImageProvider("presets/misc/no_icon")
                .setDirs(getIconSourceDirs(source))
                .setId("mappaint."+source.getPrefName())
                .setArchive(source.zipIcons)
                .setInArchiveDir(source.getZipEntryDirName())
                .setOptional(true).get();
    }

    /**
     * Returns the node icon that would be displayed for the given tag.
     * @param tag The tag to look an icon for
     * @return {@code null} if no icon found
     * @deprecated use {@link ImageProvider#getPadded}
     */
    @Deprecated
    public static ImageIcon getNodeIcon(Tag tag) {
        if (tag != null) {
            DataSet ds = new DataSet();
            Node virtualNode = new Node(LatLon.ZERO);
            virtualNode.put(tag.getKey(), tag.getValue());
            MapCSSStyleSource.STYLE_SOURCE_LOCK.readLock().lock();
            try {
                // Add primitive to dataset to avoid DataIntegrityProblemException when evaluating selectors
                ds.addPrimitive(virtualNode);
                return ImageProvider.getPadded(virtualNode, ImageProvider.ImageSizes.SMALLICON.getImageDimension(),
                        EnumSet.of(ImageProvider.GetPaddedOptions.NO_PRESETS, ImageProvider.GetPaddedOptions.NO_DEFAULT));
            } finally {
                ds.removePrimitive(virtualNode);
                MapCSSStyleSource.STYLE_SOURCE_LOCK.readLock().unlock();
            }
        }
        return null;
    }

    /**
     * Gets the directories that should be searched for icons
     * @param source The style source the icon is from
     * @return A list of directory names
     */
    public static List<String> getIconSourceDirs(StyleSource source) {
        List<String> dirs = new LinkedList<>();

        File sourceDir = source.getLocalSourceDir();
        if (sourceDir != null) {
            dirs.add(sourceDir.getPath());
        }

        Collection<String> prefIconDirs = Config.getPref().getList("mappaint.icon.sources");
        for (String fileset : prefIconDirs) {
            String[] a;
            if (fileset.indexOf('=') >= 0) {
                a = fileset.split("=", 2);
            } else {
                a = new String[] {"", fileset};
            }

            /* non-prefixed path is generic path, always take it */
            if (a[0].isEmpty() || source.getPrefName().equals(a[0])) {
                dirs.add(a[1]);
            }
        }

        if (Config.getPref().getBoolean("mappaint.icon.enable-defaults", true)) {
            /* don't prefix icon path, as it should be generic */
            dirs.add("resource://images/");
        }

        return dirs;
    }

    /**
     * Reloads all styles from the preferences.
     */
    public static void readFromPreferences() {
        styles.clear();

        Collection<? extends SourceEntry> sourceEntries = MapPaintPrefHelper.INSTANCE.get();

        for (SourceEntry entry : sourceEntries) {
            try {
                styles.add(fromSourceEntry(entry));
            } catch (IllegalArgumentException e) {
                Logging.error("Failed to load map paint style {0}", entry);
                Logging.error(e);
            }
        }
        for (StyleSource source : styles.getStyleSources()) {
            if (source.active) {
                loadStyleForFirstTime(source);
            } else {
                source.loadStyleSource(true);
            }
        }
        fireMapPaintSylesUpdated();
    }

    private static void loadStyleForFirstTime(StyleSource source) {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        source.loadStyleSource();
        if (Config.getPref().getBoolean("mappaint.auto_reload_local_styles", true) && source.isLocal()) {
            try {
                FileWatcher.getDefaultInstance().registerSource(source);
            } catch (IOException | IllegalStateException | IllegalArgumentException e) {
                Logging.error(e);
            }
        }
        if (Logging.isDebugEnabled() || !source.isValid()) {
            String message = "Initializing map style " + source.url + " completed in " + stopwatch;
            if (!source.isValid()) {
                Logging.warn(message + " (" + source.getErrors().size() + " errors, " + source.getWarnings().size() + " warnings)");
            } else {
                Logging.debug(message);
            }
        }
    }

    private static StyleSource fromSourceEntry(SourceEntry entry) {
        if (entry.url == null && entry instanceof MapCSSStyleSource) {
            return (MapCSSStyleSource) entry;
        }
        try (CachedFile cf = new CachedFile(entry.url).setHttpAccept(MapCSSStyleSource.MAPCSS_STYLE_MIME_TYPES)) {
            String zipEntryPath = cf.findZipEntryPath("mapcss", "style");
            if (zipEntryPath != null) {
                entry.isZip = true;
                entry.zipEntryPath = zipEntryPath;
            }
            return new MapCSSStyleSource(entry);
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
        int[] selSorted = Utils.copyArray(sel);
        Arrays.sort(selSorted);
        List<StyleSource> data = new ArrayList<>(styles.getStyleSources());
        for (int row: selSorted) {
            StyleSource t1 = data.get(row);
            StyleSource t2 = data.get(row + delta);
            data.set(row, t2);
            data.set(row + delta, t1);
        }
        styles.setStyleSources(data);
        MapPaintPrefHelper.INSTANCE.put(data);
        fireMapPaintSylesUpdated();
    }

    /**
     * Check if the styles can be moved
     * @param sel The indexes of the selected styles
     * @param i The number of places to move the styles
     * @return <code>true</code> if that movement is possible
     */
    public static boolean canMoveStyles(int[] sel, int i) {
        if (sel.length == 0)
            return false;
        int[] selSorted = Utils.copyArray(sel);
        Arrays.sort(selSorted);

        if (i < 0) // Up
            return selSorted[0] >= -i;
        else if (i > 0) // Down
            return selSorted[selSorted.length-1] <= styles.getStyleSources().size() - 1 - i;
        else
            return true;
    }

    /**
     * Toggles the active state of several styles
     * @param sel The style indexes
     */
    public static void toggleStyleActive(int... sel) {
        List<StyleSource> data = styles.getStyleSources();
        for (int p : sel) {
            StyleSource s = data.get(p);
            s.active = !s.active;
            if (s.active && !s.isLoaded()) {
                loadStyleForFirstTime(s);
            }
        }
        MapPaintPrefHelper.INSTANCE.put(data);
        if (sel.length == 1) {
            fireMapPaintStyleEntryUpdated(sel[0]);
        } else {
            fireMapPaintSylesUpdated();
        }
    }

    /**
     * Add a new map paint style.
     * @param entry map paint style
     * @return loaded style source
     */
    public static StyleSource addStyle(SourceEntry entry) {
        StyleSource source = fromSourceEntry(entry);
        styles.add(source);
        loadStyleForFirstTime(source);
        refreshStyles();
        return source;
    }

    /**
     * Remove a map paint style.
     * @param entry map paint style
     * @since 11493
     */
    public static void removeStyle(SourceEntry entry) {
        StyleSource source = fromSourceEntry(entry);
        if (styles.remove(source)) {
            refreshStyles();
        }
    }

    private static void refreshStyles() {
        MapPaintPrefHelper.INSTANCE.put(styles.getStyleSources());
        fireMapPaintSylesUpdated();
    }

    /***********************************
     * MapPaintSylesUpdateListener &amp; related code
     *  (get informed when the list of MapPaint StyleSources changes)
     */
    public interface MapPaintSylesUpdateListener {
        /**
         * Called on any style source changes that are not handled by {@link #mapPaintStyleEntryUpdated(int)}
         */
        void mapPaintStylesUpdated();

        /**
         * Called whenever a single style source entry was changed.
         * @param index The index of the entry.
         */
        void mapPaintStyleEntryUpdated(int index);
    }

    /**
     * Add a listener that listens to global style changes.
     * @param listener The listener
     */
    public static void addMapPaintSylesUpdateListener(MapPaintSylesUpdateListener listener) {
        listeners.addListener(listener);
    }

    /**
     * Removes a listener that listens to global style changes.
     * @param listener The listener
     */
    public static void removeMapPaintSylesUpdateListener(MapPaintSylesUpdateListener listener) {
        listeners.removeListener(listener);
    }

    /**
     * Notifies all listeners that there was any update to the map paint styles
     */
    public static void fireMapPaintSylesUpdated() {
        listeners.fireEvent(MapPaintSylesUpdateListener::mapPaintStylesUpdated);
    }

    /**
     * Notifies all listeners that there was an update to a specific map paint style
     * @param index The style index
     */
    public static void fireMapPaintStyleEntryUpdated(int index) {
        listeners.fireEvent(l -> l.mapPaintStyleEntryUpdated(index));
    }
}
