// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences.pref;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.layer.AbstractTileSourceLayer;
import org.openstreetmap.josm.gui.layer.ImageryLayer;

public class OffsetBookmark {
    private static final List<OffsetBookmark> allBookmarks = new ArrayList<>();

    @pref private String projection_code;
    @pref private String imagery_name;
    @pref private String name;
    @pref private double dx, dy;
    @pref private double center_lon, center_lat;

    public boolean isUsable(ImageryLayer layer) {
        if (projection_code == null) return false;
        if (!Main.getProjection().toCode().equals(projection_code)) return false;
        return layer.getInfo().getName().equals(imagery_name);
    }

    /**
     * Construct new empty OffsetBookmark.
     *
     * Only used for preferences handling.
     */
    public OffsetBookmark() {
        // do nothing
    }

    public OffsetBookmark(String projectionCode, String imageryName, String name, double dx, double dy) {
        this(projectionCode, imageryName, name, dx, dy, 0, 0);
    }

    public OffsetBookmark(String projectionCode, String imageryName, String name, double dx, double dy, double centerLon, double centerLat) {
        this.projection_code = projectionCode;
        this.imagery_name = imageryName;
        this.name = name;
        this.dx = dx;
        this.dy = dy;
        this.center_lon = centerLon;
        this.center_lat = centerLat;
    }

    public OffsetBookmark(Collection<String> list) {
        List<String> array = new ArrayList<>(list);
        this.projection_code = array.get(0);
        this.imagery_name = array.get(1);
        this.name = array.get(2);
        this.dx = Double.parseDouble(array.get(3));
        this.dy = Double.parseDouble(array.get(4));
        if (array.size() >= 7) {
            this.center_lon = Double.parseDouble(array.get(5));
            this.center_lat = Double.parseDouble(array.get(6));
        }
        if (projection_code == null) {
            Main.error(tr("Projection ''{0}'' is not found, bookmark ''{1}'' is not usable", projection_code, name));
        }
    }

    public String getProjectionCode() {
        return projection_code;
    }

    public String getName() {
        return name;
    }

    public String getImageryName() {
        return imagery_name;
    }

    public EastNorth getOffset() {
        return new EastNorth(dx, dy);
    }

    public LatLon getCenter() {
        return new LatLon(center_lat, center_lon);
    }

    public void setProjectionCode(String projectionCode) {
        this.projection_code = projectionCode;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setImageryName(String imageryName) {
        this.imagery_name = imageryName;
    }

    public void setOffset(EastNorth offset) {
        this.dx = offset.east();
        this.dy = offset.north();
    }

    public static void loadBookmarks() {
        List<OffsetBookmark> bookmarks = Main.pref.getListOfStructs("imagery.offsetbookmarks", null, OffsetBookmark.class);
        if (bookmarks == null) {
            loadBookmarksOld();
            saveBookmarks();
        } else {
            allBookmarks.addAll(bookmarks);
        }
    }

    // migration code - remove Nov. 2017
    private static void loadBookmarksOld() {
        for (Collection<String> c : Main.pref.getArray("imagery.offsets",
                Collections.<Collection<String>>emptySet())) {
            allBookmarks.add(new OffsetBookmark(c));
        }
    }

    public static void saveBookmarks() {
        Main.pref.putListOfStructs("imagery.offsetbookmarks", allBookmarks, OffsetBookmark.class);
    }

    /**
     * Returns all bookmarks.
     * @return all bookmarks (unmodifiable collection)
     * @since 11651
     */
    public static List<OffsetBookmark> getBookmarks() {
        return Collections.unmodifiableList(allBookmarks);
    }

    /**
     * Returns the number of bookmarks.
     * @return the number of bookmarks
     * @since 11651
     */
    public static int getBookmarksSize() {
        return allBookmarks.size();
    }

    /**
     * Adds a bookmark.
     * @param ob bookmark to add
     * @return {@code true}
     * @since 11651
     */
    public static boolean addBookmark(OffsetBookmark ob) {
        return allBookmarks.add(ob);
    }

    /**
     * Removes a bookmark.
     * @param ob bookmark to remove
     * @return {@code true} if this list contained the specified element
     * @since 11651
     */
    public static boolean removeBookmark(OffsetBookmark ob) {
        return allBookmarks.remove(ob);
    }

    /**
     * Returns the bookmark at the given index.
     * @param index bookmark index
     * @return the bookmark at the given index
     * @throws IndexOutOfBoundsException if the index is out of range
     *         (<tt>index &lt; 0 || index &gt;= size()</tt>)
     * @since 11651
     */
    public static OffsetBookmark getBookmarkByIndex(int index) {
        return allBookmarks.get(index);
    }

    public static OffsetBookmark getBookmarkByName(ImageryLayer layer, String name) {
        for (OffsetBookmark b : allBookmarks) {
            if (b.isUsable(layer) && name.equals(b.name))
                return b;
        }
        return null;
    }

    public static void bookmarkOffset(String name, AbstractTileSourceLayer layer) {
        LatLon center;
        if (Main.isDisplayingMapView()) {
            center = Main.getProjection().eastNorth2latlon(Main.map.mapView.getCenter());
        } else {
            center = LatLon.ZERO;
        }
        OffsetBookmark nb = new OffsetBookmark(
                Main.getProjection().toCode(), layer.getInfo().getName(),
                name, layer.getDisplaySettings().getDx(), layer.getDisplaySettings().getDy(), center.lon(), center.lat());
        for (ListIterator<OffsetBookmark> it = allBookmarks.listIterator(); it.hasNext();) {
            OffsetBookmark b = it.next();
            if (b.isUsable(layer) && name.equals(b.name)) {
                it.set(nb);
                saveBookmarks();
                return;
            }
        }
        allBookmarks.add(nb);
        saveBookmarks();
    }
}
