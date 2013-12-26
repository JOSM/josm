// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol.handler;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.remotecontrol.AddTagsDialog;
import org.openstreetmap.josm.io.remotecontrol.PermissionPrefWithDefault;

/**
 * Adds a way to the current dataset. For instance, {@code /add_way?way=lat1,lon2;lat2,lon2}.
 */
public class AddWayHandler extends RequestHandler {

    /**
     * The remote control command name used to add a way.
     */
    public static final String command = "add_way";

    private final List<LatLon> allCoordinates = new ArrayList<LatLon>();

    /**
     * The place to remeber already added nodes (they are reused if needed @since 5845
     */
    Map<LatLon, Node> addedNodes;

    @Override
    public String[] getMandatoryParams() {
        return new String[]{"way"};
    }
    
    @Override
    public String[] getOptionalParams() {
        return new String[] { "addtags" };
    }

    @Override
    public String getUsage() {
        return "adds a way (given by a semicolon separated sequence of lat,lon pairs) to the current dataset";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[] {
            "/add_way?way=53.2,13.3;53.3,13.3;53.3,13.2",
            "/add_way?&addtags=building=yes&way=45.437213,-2.810792;45.437988,-2.455983;45.224080,-2.455036;45.223302,-2.809845;45.437213,-2.810792"
        };
    }
    
    @Override
    protected void handleRequest() throws RequestHandlerErrorException, RequestHandlerBadRequestException {
        GuiHelper.runInEDTAndWait(new Runnable() {
            @Override public void run() {
                addWay();
            }
        });
        // parse parameter addtags=tag1=value1|tag2=value2
        AddTagsDialog.addTags(args, sender);
    }

    @Override
    public String getPermissionMessage() {
        return tr("Remote Control has been asked to create a new way.");
    }

    @Override
    public PermissionPrefWithDefault getPermissionPref() {
        return PermissionPrefWithDefault.CREATE_OBJECTS;
    }

    @Override
    protected void validateRequest() throws RequestHandlerBadRequestException {
        allCoordinates.clear();
        for (String coordinatesString : args.get("way").split(";\\s*")) {
            String[] coordinates = coordinatesString.split(",\\s*", 2);
            if (coordinates.length < 2) {
                throw new RequestHandlerBadRequestException(
                        tr("Invalid coordinates: {0}", Arrays.toString(coordinates)));
            }
            try {
                double lat = Double.parseDouble(coordinates[0]);
                double lon = Double.parseDouble(coordinates[1]);
                allCoordinates.add(new LatLon(lat, lon));
            } catch (NumberFormatException e) {
                throw new RequestHandlerBadRequestException("NumberFormatException ("+e.getMessage()+")");
            }
        }
        if (allCoordinates.isEmpty()) {
            throw new RequestHandlerBadRequestException(tr("Empty ways"));
        } else if (allCoordinates.size() == 1) {
            throw new RequestHandlerBadRequestException(tr("One node ways"));
        }
        if (!Main.main.hasEditLayer()) {
             throw new RequestHandlerBadRequestException(tr("There is no layer opened to add way"));
        }
    }

    /**
     * Find the node with almost the same ccords in dataset or in already added nodes
     * @since 5845
     **/
    Node findOrCreateNode(LatLon ll,  List<Command> commands) {
        Node nd = null;

        if (Main.isDisplayingMapView()) {
            Point p = Main.map.mapView.getPoint(ll);
            nd = Main.map.mapView.getNearestNode(p, OsmPrimitive.isUsablePredicate);
            if (nd!=null && nd.getCoor().greatCircleDistance(ll) > Main.pref.getDouble("remote.tolerance", 0.1)) {
                nd = null; // node is too far
            }
        }

        Node prev = null;
        for (LatLon lOld: addedNodes.keySet()) {
            if (lOld.greatCircleDistance(ll) < Main.pref.getDouble("remotecontrol.tolerance", 0.1)) {
                prev = addedNodes.get(lOld);
                break;
            }
        }

        if (prev!=null) {
            nd = prev;
        } else if (nd==null) {
            nd = new Node(ll);
            // Now execute the commands to add this node.
            commands.add(new AddCommand(nd));
            addedNodes.put(ll, nd);
        }
        return nd;
    }

    /*
     * This function creates the way with given coordinates of nodes
     */
    private void addWay() {
        addedNodes = new HashMap<LatLon, Node>();
        Way way = new Way();
        List<Command> commands = new LinkedList<Command>();
        for (LatLon ll : allCoordinates) {
            Node node = findOrCreateNode(ll, commands);
            way.addNode(node);
        }
        allCoordinates.clear();
        commands.add(new AddCommand(way));
        Main.main.undoRedo.add(new SequenceCommand(tr("Add way"), commands));
        Main.main.getCurrentDataSet().setSelected(way);
        if (PermissionPrefWithDefault.CHANGE_VIEWPORT.isAllowed()) {
            AutoScaleAction.autoScale("selection");
        } else {
            Main.map.mapView.repaint();
        }
    }
}
