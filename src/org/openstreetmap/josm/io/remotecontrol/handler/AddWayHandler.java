package org.openstreetmap.josm.io.remotecontrol.handler;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.io.remotecontrol.PermissionPrefWithDefault;
import org.openstreetmap.josm.io.remotecontrol.handler.RequestHandler.RequestHandlerBadRequestException;

/**
 * Adds a way to the current dataset. For instance, {@code /add_way?way=lat1,lon2;lat2,lon2}.
 */
public class AddWayHandler extends RequestHandler {

    /**
     * The remote control command name used to add a way.
     */
    public static final String command = "add_way";
    
    private final List<LatLon> allCoordinates = new ArrayList<LatLon>();

    @Override
    public String[] getMandatoryParams() {
        return new String[]{"way"};
    }

    @Override
    protected void handleRequest() throws RequestHandlerErrorException, RequestHandlerBadRequestException {
        Way way = new Way();
        List<Command> commands = new LinkedList<Command>();
        for (LatLon ll : allCoordinates) {
            Node node = new Node(ll);
            way.addNode(node);
            commands.add(new AddCommand(node));
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
        // parse parameter addtags=tag1=value1|tag2=vlaue2
        LoadAndZoomHandler.addTags(args);        
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
}
