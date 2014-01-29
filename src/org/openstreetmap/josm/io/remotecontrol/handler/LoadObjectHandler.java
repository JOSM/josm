// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol.handler;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.LinkedList;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.DownloadPrimitiveAction;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.remotecontrol.AddTagsDialog;
import org.openstreetmap.josm.io.remotecontrol.PermissionPrefWithDefault;

/**
 * Loads OSM primitives using their ID
 * similar to the "Download object" dialog (@see DownloadPrimitiveAction}.
 * For instance, {@code /load_object?objects=n1,w2,r3[&new_layer=false&relation_members=true]}.
 */
public class LoadObjectHandler extends RequestHandler {

    /**
     * The remote control command name used to load objects using their ID.
     */
    public static final String command = "load_object";

    private final List<PrimitiveId> ps = new LinkedList<PrimitiveId>();

    @Override
    public String[] getMandatoryParams() {
        return new String[]{"objects"};
    }
    
    @Override
    public String[] getOptionalParams() {
        return new String[] {"new_layer", "addtags", "relation_members", "referrers"};
    }

    @Override
    public String getUsage() {
        return "downloads the specified objects from the server";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[] {"/load_object?new_layer=true&objects=w106159509",
            "/load_object?new_layer=true&objects=r2263653&relation_members=true",
            "/load_object?objects=n100000&referrers=false"
        };
    }

    @Override
    protected void handleRequest() throws RequestHandlerErrorException, RequestHandlerBadRequestException {
        if (!PermissionPrefWithDefault.LOAD_DATA.isAllowed()) {
            Main.info("RemoteControl: download forbidden by preferences");
        }
        if (!ps.isEmpty()) {
            final boolean newLayer = isLoadInNewLayer();
            final boolean relationMembers = Boolean.parseBoolean(args.get("relation_members"));
            final boolean referrers = args.containsKey("referrers") ? Boolean.parseBoolean(args.get("referrers")) : true;
            GuiHelper.runInEDTAndWait(new Runnable() {
                @Override public void run() {
                    DownloadPrimitiveAction.processItems(newLayer, ps, referrers, relationMembers);
                }
            });
            GuiHelper.executeByMainWorkerInEDT(new Runnable() {
                @Override
                public void run() {
                    Main.main.getCurrentDataSet().setSelected(ps);
                    AddTagsDialog.addTags(args, sender);
                    ps.clear();
                }
            });
        }
    }

    @Override
    public String getPermissionMessage() {
        return tr("Remote Control has been asked to load objects (specified by their id) from the API.");
    }

    @Override
    public PermissionPrefWithDefault getPermissionPref() {
        return PermissionPrefWithDefault.LOAD_DATA;
    }

    @Override
    protected void validateRequest() throws RequestHandlerBadRequestException {
        ps.clear();
        for (String i : args.get("objects").split(",\\s*")) {
            try {
                ps.add(SimplePrimitiveId.fromString(i));
            } catch (IllegalArgumentException e) {
                Main.warn("RemoteControl: invalid selection '"+i+"' ignored");
            }
        }
    }
}
