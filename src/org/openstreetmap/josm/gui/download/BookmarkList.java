// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.download;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.downloadtasks.ChangesetQueryTask;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.UserIdentityManager;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.UserInfo;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapViewState;
import org.openstreetmap.josm.gui.dialogs.changeset.ChangesetCacheManager;
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.ChangesetQuery;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ImageProvider.ImageSizes;
import org.openstreetmap.josm.tools.Logging;

/**
 * List class that read and save its content from the bookmark file.
 * @since 6340
 */
public class BookmarkList extends JList<BookmarkList.Bookmark> {

    /**
     * The maximum number of changeset bookmarks to maintain in list.
     * @since 12495
     */
    public static final IntegerProperty MAX_CHANGESET_BOOKMARKS = new IntegerProperty("bookmarks.changesets.max-entries", 15);

    /**
     * Class holding one bookmarkentry.
     */
    public static class Bookmark implements Comparable<Bookmark> {
        private String name;
        private Bounds area;
        private ImageIcon icon;

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
            icon = ImageProvider.get("dialogs", "bookmark");
            name = array.get(0);
            area = new Bounds(Double.parseDouble(array.get(1)), Double.parseDouble(array.get(2)),
                              Double.parseDouble(array.get(3)), Double.parseDouble(array.get(4)));
        }

        /**
         * Constructs a new empty {@code Bookmark}.
         */
        public Bookmark() {
            this(null, null);
        }

        /**
         * Constructs a new unamed {@code Bookmark} for the given area.
         * @param area The bookmark area
         */
        public Bookmark(Bounds area) {
            this(null, area);
        }

        /**
         * Constructs a new {@code Bookmark} for the given name and area.
         * @param name The bookmark name
         * @param area The bookmark area
         * @since 12495
         */
        protected Bookmark(String name, Bounds area) {
            this.icon = ImageProvider.get("dialogs", "bookmark");
            this.name = name;
            this.area = area;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public int compareTo(Bookmark b) {
            return name.toLowerCase(Locale.ENGLISH).compareTo(b.name.toLowerCase(Locale.ENGLISH));
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, area);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Bookmark bookmark = (Bookmark) obj;
            return Objects.equals(name, bookmark.name) &&
                   Objects.equals(area, bookmark.area);
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

        /**
         * Returns the bookmark icon.
         * @return the bookmark icon
         * @since 12495
         */
        public ImageIcon getIcon() {
            return icon;
        }

        /**
         * Sets the bookmark icon.
         * @param icon the bookmark icon
         * @since 12495
         */
        public void setIcon(ImageIcon icon) {
            this.icon = icon;
        }
    }

    /**
     * A specific optional bookmark for the "home location" configured on osm.org website.
     * @since 12495
     */
    public static class HomeLocationBookmark extends Bookmark {
        /**
         * Constructs a new {@code HomeLocationBookmark}.
         */
        public HomeLocationBookmark() {
            setName(tr("Home location"));
            setIcon(ImageProvider.get("help", "home", ImageSizes.SMALLICON));
            UserInfo info = UserIdentityManager.getInstance().getUserInfo();
            if (info == null) {
                throw new IllegalStateException("User not identified");
            }
            LatLon home = info.getHome();
            if (home == null) {
                throw new IllegalStateException("User home location not set");
            }
            int zoom = info.getHomeZoom();
            if (zoom <= 3) {
                // 3 is the default zoom level in OSM database, but the real zoom level was not correct
                // for a long time, see https://github.com/openstreetmap/openstreetmap-website/issues/1592
                zoom = 15;
            }
            Projection mercator = Projections.getProjectionByCode("EPSG:3857");
            setArea(MapViewState.createDefaultState(430, 400) // Size of map on osm.org user profile settings
                    .usingProjection(mercator)
                    .usingScale(Selector.GeneralSelector.level2scale(zoom) / 100)
                    .usingCenter(mercator.latlon2eastNorth(home))
                    .getViewArea()
                    .getLatLonBoundsBox());
        }
    }

    /**
     * A specific optional bookmark for the boundaries of recent changesets.
     * @since 12495
     */
    public static class ChangesetBookmark extends Bookmark {
        /**
         * Constructs a new {@code ChangesetBookmark}.
         * @param cs changeset from which the boundaries are read. Its id, name and comment are used to name the bookmark
         */
        public ChangesetBookmark(Changeset cs) {
            setName(String.format("%d - %tF - %s", cs.getId(), cs.getCreatedAt(), cs.getComment()));
            setIcon(ImageProvider.get("data", "changeset", ImageSizes.SMALLICON));
            setArea(cs.getBounds());
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
     * Loads the home location bookmark from OSM API,
     *       the manual bookmarks from preferences file,
     *       the changeset bookmarks from changeset cache.
     */
    public final void load() {
        final DefaultListModel<Bookmark> model = (DefaultListModel<Bookmark>) getModel();
        model.removeAllElements();
        UserIdentityManager im = UserIdentityManager.getInstance();
        // Add home location bookmark first, if user fully identified
        if (im.isFullyIdentified()) {
            try {
                model.addElement(new HomeLocationBookmark());
            } catch (IllegalStateException e) {
                Logging.info(e.getMessage());
                Logging.trace(e);
            }
        }
        // Then add manual bookmarks previously saved in local preferences
        Collection<Collection<String>> args = Main.pref.getArray("bookmarks", null);
        if (args != null) {
            List<Bookmark> bookmarks = new LinkedList<>();
            for (Collection<String> entry : args) {
                try {
                    bookmarks.add(new Bookmark(entry));
                } catch (IllegalArgumentException e) {
                    Logging.log(Logging.LEVEL_ERROR, tr("Error reading bookmark entry: %s", e.getMessage()), e);
                }
            }
            Collections.sort(bookmarks);
            for (Bookmark b : bookmarks) {
                model.addElement(b);
            }
        }
        // Finally add recent changeset bookmarks, if user name is known
        final int n = MAX_CHANGESET_BOOKMARKS.get();
        if (n > 0 && !im.isAnonymous()) {
            final UserInfo userInfo = im.getUserInfo();
            if (userInfo != null) {
                final ChangesetCacheManager ccm = ChangesetCacheManager.getInstance();
                final int userId = userInfo.getId();
                int found = 0;
                for (int i = 0; i < ccm.getModel().getRowCount() && found < n; i++) {
                    Changeset cs = ccm.getModel().getValueAt(i, 0);
                    if (cs.getUser().getId() == userId && cs.getBounds() != null) {
                        model.addElement(new ChangesetBookmark(cs));
                        found++;
                    }
                }
            }
        }
    }

    /**
     * Saves all manual bookmarks to the preferences file.
     */
    public final void save() {
        List<List<String>> coll = new LinkedList<>();
        for (Object o : ((DefaultListModel<Bookmark>) getModel()).toArray()) {
            if (o instanceof HomeLocationBookmark || o instanceof ChangesetBookmark) {
                continue;
            }
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
        Main.pref.putListOfLists("bookmarks", coll);
    }

    /**
     * Refreshes the changeset bookmarks.
     * @since 12495
     */
    public void refreshChangesetBookmarks() {
        final int n = MAX_CHANGESET_BOOKMARKS.get();
        if (n > 0) {
            final DefaultListModel<Bookmark> model = (DefaultListModel<Bookmark>) getModel();
            for (int i = model.getSize() - 1; i >= 0; i--) {
                if (model.get(i) instanceof ChangesetBookmark) {
                    model.remove(i);
                }
            }
            ChangesetQuery query = ChangesetQuery.forCurrentUser();
            if (!GraphicsEnvironment.isHeadless()) {
                final ChangesetQueryTask task = new ChangesetQueryTask(this, query);
                ChangesetCacheManager.getInstance().runDownloadTask(task);
                MainApplication.worker.submit(() -> {
                    if (task.isCanceled() || task.isFailed())
                        return;
                    GuiHelper.runInEDT(() -> task.getDownloadedData().stream()
                            .filter(cs -> cs.getBounds() != null)
                            .sorted(Comparator.reverseOrder())
                            .limit(n)
                            .forEachOrdered(cs -> model.addElement(new ChangesetBookmark(cs))));
                });
            }
        }
    }

    static class BookmarkCellRenderer extends JLabel implements ListCellRenderer<BookmarkList.Bookmark> {

        /**
         * Constructs a new {@code BookmarkCellRenderer}.
         */
        BookmarkCellRenderer() {
            setOpaque(true);
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
            if (area != null) {
                sb.append("<html>min[latitude,longitude]=<strong>[")
                  .append(area.getMinLat()).append(',').append(area.getMinLon()).append("]</strong>"+
                          "<br>max[latitude,longitude]=<strong>[")
                  .append(area.getMaxLat()).append(',').append(area.getMaxLon()).append("]</strong>"+
                          "</html>");
            }
            return sb.toString();
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Bookmark> list, Bookmark value, int index, boolean isSelected,
                boolean cellHasFocus) {
            renderColor(isSelected);
            setIcon(value.getIcon());
            setText(value.getName());
            setToolTipText(buildToolTipText(value));
            return this;
        }
    }
}
