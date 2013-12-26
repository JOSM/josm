// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol.handler;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Point;
import java.util.Map;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.remotecontrol.AddTagsDialog;
import org.openstreetmap.josm.io.remotecontrol.PermissionPrefWithDefault;

/**
 * Handler for add_node request.
 */
public class AddNodeHandler extends RequestHandler {

    /**
     * The remote control command name used to add a node.
     */
    public static final String command = "add_node";

    private double lat;
    private double lon;

    @Override
    protected void handleRequest() {
        GuiHelper.runInEDTAndWait(new Runnable() {
            @Override public void run() {
                 addNode(args);
            }
        });
    }

    @Override
    public String[] getMandatoryParams()
    {
        return new String[] { "lat", "lon" };
    }
    
    @Override
    public String[] getOptionalParams()
    {
        return new String[] { "addtags" };
    }

    @Override
    public String getUsage() {
        return "adds a node (given by its latitude and longitude) to the current dataset";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[] {
            "/add_node?lat=11&lon=22",
            "/add_node?lon=13.3&lat=53.2&addtags=natural=tree|name=%20%20%20==Great%20Oak==" 
        };
    }
    
    @Override
    public String getPermissionMessage() {
        return tr("Remote Control has been asked to create a new node.") +
                "<br>" + tr("Coordinates: ") + args.get("lat") + ", " + args.get("lon");
    }

    @Override
    public PermissionPrefWithDefault getPermissionPref() {
        return PermissionPrefWithDefault.CREATE_OBJECTS;
    }

    /**
     * Adds a node, implements the GET /add_node?lon=...&amp;lat=... request.
     * @param args
     */
    private void addNode(Map<String, String> args){

        // Parse the arguments
        Main.info("Adding node at (" + lat + ", " + lon + ")");

        // Create a new node
        LatLon ll = new LatLon(lat, lon);

        Node nd = null;

        if (Main.isDisplayingMapView()) {
            Point p = Main.map.mapView.getPoint(ll);
            nd = Main.map.mapView.getNearestNode(p, OsmPrimitive.isUsablePredicate);
            if (nd!=null && nd.getCoor().greatCircleDistance(ll) > Main.pref.getDouble("remotecontrol.tolerance", 0.1)) {
                nd = null; // node is too far
            }
        }

        if (nd==null) {
            nd = new Node(ll);
            // Now execute the commands to add this node.
            Main.main.undoRedo.add(new AddCommand(nd));
        }

        Main.main.getCurrentDataSet().setSelected(nd);
        if (PermissionPrefWithDefault.CHANGE_VIEWPORT.isAllowed()) {
            AutoScaleAction.autoScale("selection");
        } else {
            Main.map.mapView.repaint();
        }
        // parse parameter addtags=tag1=value1|tag2=vlaue2
        AddTagsDialog.addTags(args, sender);
    }

    @Override
    protected void validateRequest() throws RequestHandlerBadRequestException {
        try {
            lat = Double.parseDouble(args.get("lat"));
            lon = Double.parseDouble(args.get("lon"));
        } catch (NumberFormatException e) {
            throw new RequestHandlerBadRequestException("NumberFormatException ("+e.getMessage()+")");
        }
        if (!Main.main.hasEditLayer()) {
             throw new RequestHandlerBadRequestException(tr("There is no layer opened to add node"));
        }
    }
}
