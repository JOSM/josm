// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.layer.ImageryLayer;

public class OffsetBookmark {
    public static List<OffsetBookmark> allBookmarks = new ArrayList<OffsetBookmark>();

    public Projection proj;
    public String layerName;
    public String name;
    public double dx, dy;
    public double centerX, centerY;

    public boolean isUsable(ImageryLayer layer) {
        return Main.proj.getClass() == proj.getClass() &&
        layer.getInfo().getName().equals(layerName);
    }

    public OffsetBookmark(Projection proj, String layerName, String name, double dx, double dy) {
        this(proj, layerName, name, dx, dy, 0, 0);
    }

    public OffsetBookmark(Projection proj, String layerName, String name, double dx, double dy, double centerX, double centerY) {
        this.proj = proj;
        this.layerName = layerName;
        this.name = name;
        this.dx = dx;
        this.dy = dy;
        this.centerX = centerX;
        this.centerY = centerY;
    }

    public OffsetBookmark(Collection<String> list) {
        ArrayList<String> array = new ArrayList<String>(list);
        String projectionName = array.get(0);
        for (Projection proj : Projection.allProjections) {
            if (proj.getCacheDirectoryName().equals(projectionName)) {
                this.proj = proj;
                break;
            }
        }
        if (this.proj == null)
            throw new IllegalStateException(tr("Projection ''{0}'' not found", projectionName));
        this.layerName = array.get(1);
        this.name = array.get(2);
        this.dx = Double.valueOf(array.get(3));
        this.dy = Double.valueOf(array.get(4));
        if (array.size() >= 7) {
            this.centerX = Double.valueOf(array.get(5));
            this.centerY = Double.valueOf(array.get(6));
        }
    }

    public ArrayList<String> getInfoArray() {
        ArrayList<String> res = new ArrayList<String>(5);
        res.add(proj.getCacheDirectoryName()); // we should use non-localized projection name
        res.add(layerName);
        res.add(name);
        res.add(String.valueOf(dx));
        res.add(String.valueOf(dy));
        if (this.centerX != 0 || this.centerY != 0) {
            res.add(String.valueOf(centerX));
            res.add(String.valueOf(centerY));
        }
        return res;
    }

    public static void loadBookmarks() {
        for(Collection<String> c : Main.pref.getArray("imagery.offsets",
                Collections.<Collection<String>>emptySet())) {
            allBookmarks.add(new OffsetBookmark(c));
        }
    }

    public static void saveBookmarks() {
        LinkedList<Collection<String>> coll = new LinkedList<Collection<String>>();
        for (OffsetBookmark b : allBookmarks) {
            coll.add(b.getInfoArray());
        }
        Main.pref.putArray("imagery.offsets", coll);
    }

    public static OffsetBookmark getBookmarkByName(ImageryLayer layer, String name) {
        for (OffsetBookmark b : allBookmarks) {
            if (b.isUsable(layer) && name.equals(b.name))
                return b;
        }
        return null;
    }

    public static void bookmarkOffset(String name, ImageryLayer layer) {
        LatLon center;
        if (Main.map != null && Main.map.mapView != null) {
            center = Main.proj.eastNorth2latlon(Main.map.mapView.getCenter());
        } else {
            center = new LatLon(0,0);
        }
        OffsetBookmark nb = new OffsetBookmark(
                Main.proj, layer.getInfo().getName(),
                name, layer.getDx(), layer.getDy(), center.lon(), center.lat());
        for (ListIterator<OffsetBookmark> it = allBookmarks.listIterator();it.hasNext();) {
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
