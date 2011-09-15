// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * @author Vincent
 *
 */
public class Shape {

    private List<Coordinate> coords = new ArrayList<Coordinate>();
    
    public Shape(String asString, String separator) throws IllegalArgumentException {
        CheckParameterUtil.ensureParameterNotNull(asString, "asString");
        String[] components = asString.split(separator);
        if (components.length % 2 != 0)
            throw new IllegalArgumentException(MessageFormat.format("Even number of doubles excpected in string, got {0}: {1}", components.length, asString));
        for (int i=0; i<components.length; i+=2) {
            addPoint(components[i], components[i+1]);
        }
    }

    public Shape() {
    }

    public String encodeAsString(String separator) {
        StringBuffer sb = new StringBuffer();
        for (Coordinate c : coords) {
            if (sb.length() != 0) {
                sb.append(separator);
            }
            sb.append(c.getLat()).append(separator).append(c.getLon());
        }
        return sb.toString();
    }

    public List<Coordinate> getPoints() {
        return coords;
    }

    public void addPoint(String sLat, String sLon) throws IllegalArgumentException {
        CheckParameterUtil.ensureParameterNotNull(sLat, "sLat");
        CheckParameterUtil.ensureParameterNotNull(sLon, "sLon");

        double lat, lon;
        
        try {
            lat = Double.parseDouble(sLat);
            if (!LatLon.isValidLat(lat))
                throw new IllegalArgumentException(tr("Illegal latitude value ''{0}''", lat));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(MessageFormat.format("Illegal double value ''{0}''", sLat));
        }

        try {
            lon = Double.parseDouble(sLon);
            if (!LatLon.isValidLon(lon))
                throw new IllegalArgumentException(tr("Illegal longitude value ''{0}''", lon));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(MessageFormat.format("Illegal double value ''{0}''", sLon));
        }
        
        coords.add(new Coordinate(LatLon.roundToOsmPrecision(lat), LatLon.roundToOsmPrecision(lon)));
    }
}
