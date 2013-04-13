// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol.handler;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.HashMap;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.remotecontrol.PermissionPrefWithDefault;
import org.openstreetmap.josm.io.remotecontrol.handler.RequestHandler.RequestHandlerBadRequestException;

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
    private void addNode(HashMap<String, String> args){

        // Parse the arguments
        System.out.println("Adding node at (" + lat + ", " + lon + ")");

        // Create a new node
        LatLon ll = new LatLon(lat, lon);
        Node nnew = new Node(ll);

        // Now execute the commands to add this node.
        Main.main.undoRedo.add(new AddCommand(nnew));
        Main.main.getCurrentDataSet().setSelected(nnew);
        if (PermissionPrefWithDefault.CHANGE_VIEWPORT.isAllowed()) {
            AutoScaleAction.autoScale("selection");
        } else {
            Main.map.mapView.repaint();
        }
        // parse parameter addtags=tag1=value1|tag2=vlaue2
        LoadAndZoomHandler.addTags(args);
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
