package org.openstreetmap.josm.io.remotecontrol.handler;

import static org.openstreetmap.josm.tools.I18n.tr;

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

/**
 * Adds a way to the current dataset. For instance, {@code /add_way?way=lat1,lon2;lat2,lon2}.
 */
public class AddWayHandler extends RequestHandler {

    public static final String command = "add_way";

    @Override
    public String[] getMandatoryParams() {
        return new String[]{"way"};
    }

    @Override
    protected void handleRequest() throws RequestHandlerErrorException, RequestHandlerBadRequestException {
        Way way = new Way();
        List<Command> commands = new LinkedList<Command>();
        for (String coordinatesString : args.get("way").split(";\\s*")) {
            String[] coordinates = coordinatesString.split(",\\s*", 2);
            double lat = Double.parseDouble(coordinates[0]);
            double lon = Double.parseDouble(coordinates[1]);
            Node node = new Node(new LatLon(lat, lon));
            way.addNode(node);
            commands.add(new AddCommand(node));
        }
        commands.add(new AddCommand(way));
        Main.main.undoRedo.add(new SequenceCommand(tr("Add way"), commands));
        Main.main.getCurrentDataSet().setSelected(way);
        if (Main.pref.getBoolean(LoadAndZoomHandler.changeViewportPermissionKey, LoadAndZoomHandler.changeViewportPermissionDefault)) {
            new AutoScaleAction("selection").actionPerformed(null);
        } else {
            Main.map.mapView.repaint();
        }
    }

    @Override
    public String getPermissionMessage() {
        return tr("Remote Control has been asked to create a new way.");
    }
}
