package org.openstreetmap.josm.io.remotecontrol.handler;

import java.util.LinkedList;
import java.util.List;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.DownloadPrimitiveAction;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;

/**
 * Loads OSM primitives using their ID
 * similar to the "Download object" dialog (@see DownloadPrimitiveAction}.
 * For instance, {@code /load_object?objects=n1,w2,r3[&new_layer=false&relation_members=true]}.
 */
public class LoadObject extends RequestHandler {

    public static final String command = "load_object";

    public LoadObject() {
    }

    @Override
    protected String[] getMandatoryParams() {
        return new String[]{"objects"};
    }

    @Override
    protected void handleRequest() throws RequestHandlerErrorException, RequestHandlerBadRequestException {
        if (!Main.pref.getBoolean(LoadAndZoomHandler.loadDataPermissionKey, LoadAndZoomHandler.loadDataPermissionDefault)) {
            System.out.println("RemoteControl: download forbidden by preferences");
        }
        List<PrimitiveId> ps = new LinkedList<PrimitiveId>();
        for (String i : args.get("objects").split(",\\s*")) {
            ps.add(SimplePrimitiveId.fromString(i));
        }
        boolean newLayer = Boolean.parseBoolean(args.get("new_layer"));
        boolean relationMembers = Boolean.parseBoolean(args.get("relation_members"));
        DownloadPrimitiveAction.processItems(newLayer, ps, true, relationMembers);
    }

    @Override
    public String getPermissionMessage() {
        return "";
    }
}
