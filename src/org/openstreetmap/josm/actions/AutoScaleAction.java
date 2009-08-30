// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Toggles the autoScale feature of the mapView
 * @author imi
 */
public class AutoScaleAction extends JosmAction {

    public static final String[] modes = { marktr("data"), marktr("layer"), marktr("selection"), marktr("conflict"), marktr("download") };
    private final String mode;

    private static int getModeShortcut(String mode) {
        int shortcut = -1;

        if (mode.equals("data")) {
            shortcut = KeyEvent.VK_1;
        }
        if (mode.equals("layer")) {
            shortcut = KeyEvent.VK_2;
        }
        if (mode.equals("selection")) {
            shortcut = KeyEvent.VK_3;
        }
        if (mode.equals("conflict")) {
            shortcut = KeyEvent.VK_4;
        }
        if (mode.equals("download")) {
            shortcut = KeyEvent.VK_5;
        }

        return shortcut;
    }

    public AutoScaleAction(String mode) {
        super(tr("Zoom to {0}", tr(mode)), "dialogs/autoscale/" + mode, tr("Zoom the view to {0}.", tr(mode)),
                Shortcut.registerShortcut("view:zoom"+mode, tr("View: {0}", tr("Zoom to {0}", tr(mode))), getModeShortcut(mode), Shortcut.GROUP_EDIT), true);
        String modeHelp = Character.toUpperCase(mode.charAt(0)) + mode.substring(1);
        putValue("help", "Action/AutoScale/" + modeHelp);
        this.mode = mode;
    }

    public void autoScale()  {
        if (Main.map != null) {
            BoundingXYVisitor bbox = getBoundingBox();
            if (bbox != null && bbox.getBounds() != null) {
                Main.map.mapView.recalculateCenterScale(bbox);
            }
        }
        putValue("active", true);
    }

    public void actionPerformed(ActionEvent e) {
        autoScale();
    }

    protected Layer getActiveLayer() {
        try {
            return Main.map.mapView.getActiveLayer();
        } catch(NullPointerException e) {
            return null;
        }
    }

    /**
     * Replies the first selected layer in the layer list dialog. null, if no
     * such layer exists, either because the layer list dialog is not yet created
     * or because no layer is selected.
     * 
     * @return the first selected layer in the layer list dialog
     */
    protected Layer getFirstSelectedLayer() {
        if (LayerListDialog.getInstance() == null) return null;
        List<Layer> layers = LayerListDialog.getInstance().getModel().getSelectedLayers();
        if (layers.isEmpty()) return null;
        return layers.get(0);
    }

    private BoundingXYVisitor getBoundingBox() {
        BoundingXYVisitor v = new BoundingXYVisitor();
        if (mode.equals("data")) {
            for (Layer l : Main.map.mapView.getAllLayers()) {
                l.visitBoundingBox(v);
            }
        } else if (mode.equals("layer")) {
            if (getActiveLayer() == null)
                return null;
            // try to zoom to the first selected layer
            //
            Layer l = getFirstSelectedLayer();
            if (l == null) return null;
            l.visitBoundingBox(v);
        } else if (mode.equals("selection") || mode.equals("conflict")) {
            Collection<OsmPrimitive> sel = new HashSet<OsmPrimitive>();
            if (mode.equals("selection")) {
                sel = getCurrentDataSet().getSelected();
            } else if (mode.equals("conflict")) {
                if (Main.map.conflictDialog.getConflicts() != null) {
                    sel = Main.map.conflictDialog.getConflicts().getMyConflictParties();
                }
            }
            if (sel.isEmpty()) {
                JOptionPane.showMessageDialog(
                        Main.parent,
                        (mode.equals("selection") ? tr("Nothing selected to zoom to.") : tr("No conflicts to zoom to")),
                        tr("Information"),
                        JOptionPane.INFORMATION_MESSAGE
                );
                return null;
            }
            for (OsmPrimitive osm : sel) {
                osm.visit(v);
            }
            // increase bbox by 0.001 degrees on each side. this is required
            // especially if the bbox contains one single node, but helpful
            // in most other cases as well.
            v.enlargeBoundingBox();
        }
        else if (mode.equals("download")) {
            if (Main.pref.hasKey("osm-download.bounds")) {
                try {
                    String bounds[] = Main.pref.get("osm-download.bounds").split(";");
                    double minlat = Double.parseDouble(bounds[0]);
                    double minlon = Double.parseDouble(bounds[1]);
                    double maxlat = Double.parseDouble(bounds[2]);
                    double maxlon = Double.parseDouble(bounds[3]);

                    v.visit(Main.proj.latlon2eastNorth(new LatLon(minlat, minlon)));
                    v.visit(Main.proj.latlon2eastNorth(new LatLon(maxlat, maxlon)));
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return v;
    }

    @Override
    protected void updateEnabledState() {
        if ("selection".equals(mode)) {
            setEnabled(getCurrentDataSet() != null && ! getCurrentDataSet().getSelected().isEmpty());
        }  else if ("layer".equals(mode)) {
            if (Main.map == null || Main.map.mapView == null || Main.map.mapView.getAllLayersAsList().isEmpty()) {
                setEnabled(false);
            } else {
                // FIXME: should also check for whether a layer is selected in the layer list dialog
                setEnabled(true);
            }
        } else {
            setEnabled(
                    Main.map != null
                    && Main.map.mapView != null
                    && Main.map.mapView.hasLayers()
            );
        }
    }
}
