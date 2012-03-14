package org.openstreetmap.josm.io.remotecontrol.handler;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.LinkedList;
import java.util.List;
import org.openstreetmap.josm.actions.DownloadPrimitiveAction;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.io.remotecontrol.PermissionPrefWithDefault;

/**
 * Loads OSM primitives using their ID
 * similar to the "Download object" dialog (@see DownloadPrimitiveAction}.
 * For instance, {@code /load_object?objects=n1,w2,r3[&new_layer=false&relation_members=true]}.
 */
public class LoadObjectHandler extends RequestHandler {

    public static final String command = "load_object";

    @Override
    public String[] getMandatoryParams() {
        return new String[]{"objects"};
    }

    @Override
    protected void handleRequest() throws RequestHandlerErrorException, RequestHandlerBadRequestException {
        if (!PermissionPrefWithDefault.LOAD_DATA.isAllowed()) {
            System.out.println("RemoteControl: download forbidden by preferences");
        }
        List<PrimitiveId> ps = new LinkedList<PrimitiveId>();
        for (String i : args.get("objects").split(",\\s*")) {
            ps.add(SimplePrimitiveId.fromString(i));
        }
        boolean newLayer = isLoadInNewLayer();
        boolean relationMembers = Boolean.parseBoolean(args.get("relation_members"));
        DownloadPrimitiveAction.processItems(newLayer, ps, true, relationMembers);
    }

    @Override
    public String getPermissionMessage() {
        return tr("Remote Control has been asked to load objects (specified by their id) from the API.");
    }

    @Override
    public PermissionPrefWithDefault getPermissionPref() {
        return PermissionPrefWithDefault.LOAD_DATA;
    }
}
