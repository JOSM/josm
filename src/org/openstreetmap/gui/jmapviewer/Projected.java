// License: GPL. For details, see Readme.txt file.
package org.openstreetmap.gui.jmapviewer;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Objects;

import org.openstreetmap.gui.jmapviewer.interfaces.IProjected;

/**
 * Projected coordinates represented by an encapsulates a Point2D.Double value.
 */
public class Projected implements IProjected {
    private transient Point2D.Double data;

    /**
     * Constructs a new {@code Projected}.
     * @param east easting
     * @param north northing
     */
    public Projected(double east, double north) {
        data = new Point2D.Double(east, north);
    }

    @Override
    public double getEast() {
        return data.x;
    }

    @Override
    public double getNorth() {
        return data.y;
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
        return "Projected[" + data.y + ", " + data.x + ']';
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(data);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || !(obj instanceof Projected))
            return false;
        final Projected other = (Projected) obj;
        return Objects.equals(data, other.data);
    }
}
