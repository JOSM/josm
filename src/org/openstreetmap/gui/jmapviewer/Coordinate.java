// License: GPL. For details, see Readme.txt file.
package org.openstreetmap.gui.jmapviewer;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Objects;

import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;

/**
 * This class encapsulates a Point2D.Double and provide access
 * via <code>lat</code> and <code>lon</code>.
 *
 * @author Jan Peter Stotz
 *
 */
public class Coordinate implements ICoordinate {
    private transient Point2D.Double data;

    /**
     * Constructs a new {@code Coordinate}.
     * @param lat latitude in degrees
     * @param lon longitude in degrees
     */
    public Coordinate(double lat, double lon) {
        data = new Point2D.Double(lon, lat);
    }

    @Override
    public double getLat() {
        return data.y;
    }

    @Override
    public void setLat(double lat) {
        data.y = lat;
    }

    @Override
    public double getLon() {
        return data.x;
    }

    @Override
    public void setLon(double lon) {
        data.x = lon;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(data.x);
        out.writeObject(data.y);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        data = new Point2D.Double();
        data.x = (Double) in.readObject();
        data.y = (Double) in.readObject();
    }

    @Override
    public String toString() {
        return "Coordinate[" + data.y + ", " + data.x + ']';
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(data);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || !(obj instanceof Coordinate))
            return false;
        final Coordinate other = (Coordinate) obj;
        return Objects.equals(data, other.data);
    }
}
