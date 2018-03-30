// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.StructUtils;
import org.openstreetmap.josm.data.StructUtils.StructEntry;
import org.openstreetmap.josm.data.StructUtils.WriteExplicitly;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.AbstractTileSourceLayer;
import org.openstreetmap.josm.gui.layer.ImageryLayer;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;

/**
 * Class to save a displacement of background imagery as a bookmark.
 *
 * Known offset bookmarks will be stored in the preferences and can be
 * restored by the user in later sessions.
 */
public class OffsetBookmark {
    private static final List<OffsetBookmark> allBookmarks = new ArrayList<>();

    @StructEntry private String projection_code;
    @StructEntry private String imagery_name;
    @StructEntry private String name;
    @StructEntry @WriteExplicitly private double dx, dy;
    @StructEntry private double center_lon, center_lat;

    /**
     * Test if an image is usable for the given imagery layer.
     * @param layer The layer to use the image at
     * @return <code>true</code> if it is usable on the projection of the layer and the imagery name matches.
     */
    public boolean isUsable(ImageryLayer layer) {
        if (projection_code == null) return false;
        if (!Main.getProjection().toCode().equals(projection_code) && !hasCenter()) return false;
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

    /**
     * Create a new {@link OffsetBookmark} object using (0, 0) as center
     * <p>
     * The use of the {@link #OffsetBookmark(String, String, String, EastNorth, ILatLon)} constructor is preferred.
     * @param projectionCode The projection for which this object was created
     * @param imageryName The name of the imagery on the layer
     * @param name The name of the new bookmark
     * @param dx The x displacement
     * @param dy The y displacement
     */
    public OffsetBookmark(String projectionCode, String imageryName, String name, double dx, double dy) {
        this(projectionCode, imageryName, name, dx, dy, 0, 0);
    }

    /**
     * Create a new {@link OffsetBookmark} object
     * @param projectionCode The projection for which this object was created
     * @param imageryName The name of the imagery on the layer
     * @param name The name of the new bookmark
     * @param displacement The displacement in east/north space.
     * @param center The point on earth that was used as reference to align the image.
     * @since 13243
     */
    public OffsetBookmark(String projectionCode, String imageryName, String name, EastNorth displacement, ILatLon center) {
        this(projectionCode, imageryName, name, displacement.east(), displacement.north(), center.lon(), center.lat());
    }

    /**
     * Create a new {@link OffsetBookmark} by specifying all values.
     * <p>
     * The use of the {@link #OffsetBookmark(String, String, String, EastNorth, ILatLon)} constructor is preferred.
     * @param projectionCode The projection for which this object was created
     * @param imageryName The name of the imagery on the layer
     * @param name The name of the new bookmark
     * @param dx The x displacement
     * @param dy The y displacement
     * @param centerLon The point on earth that was used as reference to align the image.
     * @param centerLat The point on earth that was used as reference to align the image.
     */
    public OffsetBookmark(String projectionCode, String imageryName, String name, double dx, double dy, double centerLon, double centerLat) {
        this.projection_code = projectionCode;
        this.imagery_name = imageryName;
        this.name = name;
        this.dx = dx;
        this.dy = dy;
        this.center_lon = centerLon;
        this.center_lat = centerLat;
    }

    /**
     * Loads an old bookmark. For backward compatibility with settings. Do not use.
     * @param list The settings that were read
     */
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
            Logging.error(tr("Projection ''{0}'' is not found, bookmark ''{1}'' is not usable", projection_code, name));
        }
    }

    /**
     * Get the projection code for which this bookmark was created.
     * @return The projection.
     */
    public String getProjectionCode() {
        return projection_code;
    }

    /**
     * Get the name of this bookmark. This name can e.g. be displayed in menus.
     * @return The name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the name of the imagery for which this bookmark was created. It is used to match the bookmark to the right layers.
     * @return The name
     */
    public String getImageryName() {
        return imagery_name;
    }

    /**
     * Get displacement in EastNorth coordinates of the original projection.
     *
     * @return the displacement
     * @see #getProjectionCode()
     */
    public EastNorth getDisplacement() {
        return new EastNorth(dx, dy);
    }

    /**
     * Get displacement in EastNorth coordinates of a given projection.
     *
     * Displacement will be converted to the given projection, with respect to the
     * center (reference point) of this bookmark.
     * @param proj the projection
     * @return the displacement, converted to that projection
     */
    public EastNorth getDisplacement(Projection proj) {
        if (proj.toCode().equals(projection_code)) {
            return getDisplacement();
        }
        LatLon center = getCenter();
        Projection offsetProj = Projections.getProjectionByCode(projection_code);
        EastNorth centerEN = center.getEastNorth(offsetProj);
        EastNorth shiftedEN = centerEN.add(getDisplacement());
        LatLon shifted = offsetProj.eastNorth2latlon(shiftedEN);
        EastNorth centerEN2 = center.getEastNorth(proj);
        EastNorth shiftedEN2 = shifted.getEastNorth(proj);
        return shiftedEN2.subtract(centerEN2);
    }

    /**
     * Get center/reference point of the bookmark.
     *
     * Basically this is the place where it was created and is valid.
     * The center may be unrecorded (see {@link #hasCenter()}, in which
     * case a dummy center (0,0) will be returned.
     * @return the center
     */
    public LatLon getCenter() {
        return new LatLon(center_lat, center_lon);
    }

    /**
     * Check if bookmark has a valid center.
     * @return true if bookmark has a valid center
     */
    public boolean hasCenter() {
        return center_lat != 0 || center_lon != 0;
    }

    /**
     * Set the projection code for which this bookmark was created
     * @param projectionCode The projection
     */
    public void setProjectionCode(String projectionCode) {
        this.projection_code = projectionCode;
    }

    /**
     * Set the name of the bookmark
     * @param name The name
     * @see #getName()
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the name of the imagery
     * @param imageryName The name
     * @see #getImageryName()
     */
    public void setImageryName(String imageryName) {
        this.imagery_name = imageryName;
    }

    /**
     * Update the displacement of this imagery.
     * @param displacement The displacement
     */
    public void setDisplacement(EastNorth displacement) {
        this.dx = displacement.east();
        this.dy = displacement.north();
    }

    /**
     * Load the global list of bookmarks from preferences.
     */
    public static void loadBookmarks() {
        List<OffsetBookmark> bookmarks = StructUtils.getListOfStructs(
                Config.getPref(), "imagery.offsetbookmarks", null, OffsetBookmark.class);
        if (bookmarks == null) {
            loadBookmarksOld();
            saveBookmarks();
        } else {
            allBookmarks.addAll(bookmarks);
        }
    }

    // migration code - remove Nov. 2017
    private static void loadBookmarksOld() {
        for (Collection<String> c : Config.getPref().getListOfLists("imagery.offsets")) {
            allBookmarks.add(new OffsetBookmark(c));
        }
    }

    /**
     * Stores the bookmakrs in the settings.
     */
    public static void saveBookmarks() {
        StructUtils.putListOfStructs(Config.getPref(), "imagery.offsetbookmarks", allBookmarks, OffsetBookmark.class);
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
     *         (<code>index &lt; 0 || index &gt;= size()</code>)
     * @since 11651
     */
    public static OffsetBookmark getBookmarkByIndex(int index) {
        return allBookmarks.get(index);
    }

    /**
     * Gets a bookmark that is usable on the given layer by it's name.
     * @param layer The layer to use the bookmark at
     * @param name The name of the bookmark
     * @return The bookmark if found, <code>null</code> if not.
     */
    public static OffsetBookmark getBookmarkByName(ImageryLayer layer, String name) {
        for (OffsetBookmark b : allBookmarks) {
            if (b.isUsable(layer) && name.equals(b.name))
                return b;
        }
        return null;
    }

    /**
     * Add a bookmark for the displacement of that layer
     * @param name The bookmark name
     * @param layer The layer to store the bookmark for
     */
    public static void bookmarkOffset(String name, AbstractTileSourceLayer<?> layer) {
        LatLon center;
        if (MainApplication.isDisplayingMapView()) {
            center = Main.getProjection().eastNorth2latlon(MainApplication.getMap().mapView.getCenter());
        } else {
            center = LatLon.ZERO;
        }
        OffsetBookmark nb = new OffsetBookmark(
                Main.getProjection().toCode(), layer.getInfo().getName(),
                name, layer.getDisplaySettings().getDisplacement(), center);
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

    /**
     * Converts the offset bookmark to a properties map.
     *
     * The map contains all the information to restore the offset bookmark.
     * @return properties map of all data
     * @see #fromPropertiesMap(java.util.Map)
     * @since 12134
     */
    public Map<String, String> toPropertiesMap() {
        return StructUtils.serializeStruct(this, OffsetBookmark.class);
    }

    /**
     * Creates an offset bookmark from a properties map.
     * @param properties the properties map
     * @return corresponding offset bookmark
     * @see #toPropertiesMap()
     * @since 12134
     */
    public static OffsetBookmark fromPropertiesMap(Map<String, String> properties) {
        return StructUtils.deserializeStruct(properties, OffsetBookmark.class);
    }
}
