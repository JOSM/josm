// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data;

import java.util.HashSet;
import java.util.Set;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

// FIXME: class still needed?

public class DataSetChecker {

    public static void check() {
        if (Main.map == null)
            return;

        Set<OsmPrimitive> s = new HashSet<OsmPrimitive>();
        for (Layer l : Main.map.mapView.getAllLayers()) {
            if (l instanceof OsmDataLayer) {
                for (OsmPrimitive osm : ((OsmDataLayer)l).data.allPrimitives()) {
                    if (s.contains(osm)) {
                        // FIXME: better message
                        // FIXME: translate message and title
                        JOptionPane.showMessageDialog(
                                Main.parent,
                                "cross references",
                                "Information",
                                JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                    s.add(osm);
                }
            }
        }

        if (Main.map.mapView.getActiveLayer() instanceof OsmDataLayer) {
            OsmDataLayer l = (OsmDataLayer)Main.map.mapView.getActiveLayer();
            if (l.data != Main.main.getCurrentDataSet()) {
                JOptionPane.showMessageDialog(
                        Main.parent,
                        "Main.ds / active layer mismatch",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }
        }

        JOptionPane.showMessageDialog(
                Main.parent, "working", "", JOptionPane.INFORMATION_MESSAGE);
    }
}
