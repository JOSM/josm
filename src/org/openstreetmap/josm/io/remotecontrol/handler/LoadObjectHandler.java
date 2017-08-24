// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol.handler;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.io.DownloadPrimitivesWithReferrersTask;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.remotecontrol.AddTagsDialog;
import org.openstreetmap.josm.io.remotecontrol.PermissionPrefWithDefault;
import org.openstreetmap.josm.tools.Logging;

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

    private final List<PrimitiveId> ps = new LinkedList<>();

    @Override
    public String[] getMandatoryParams() {
        return new String[]{"objects"};
    }

    @Override
    public String[] getOptionalParams() {
        return new String[] {"new_layer", "layer_name", "addtags", "relation_members", "referrers"};
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
            Logging.info("RemoteControl: download forbidden by preferences");
        }
        if (!ps.isEmpty()) {
            final boolean newLayer = isLoadInNewLayer();
            final boolean relationMembers = Boolean.parseBoolean(args.get("relation_members"));
            final boolean referrers = Boolean.parseBoolean(args.get("referrers"));
            final DownloadPrimitivesWithReferrersTask task = new DownloadPrimitivesWithReferrersTask(
                    newLayer, ps, referrers, relationMembers, args.get("layer_name"), null);
            MainApplication.worker.submit(task);
            MainApplication.worker.submit(() -> {
                final List<PrimitiveId> downloaded = task.getDownloadedId();
                final DataSet ds = Main.getLayerManager().getEditDataSet();
                if (downloaded != null) {
                    GuiHelper.runInEDT(() -> ds.setSelected(downloaded));
                    Collection<OsmPrimitive> downlPrim = new HashSet<>();
                    for (PrimitiveId id : downloaded) {
                        downlPrim.add(ds.getPrimitiveById(id));
                    }
                    AddTagsDialog.addTags(args, sender, downlPrim);
                }
                ps.clear();
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
        for (String i : (args != null ? args.get("objects") : "").split(",\\s*")) {
            if (!i.isEmpty()) {
                try {
                    ps.add(SimplePrimitiveId.fromString(i));
                } catch (IllegalArgumentException e) {
                    Logging.log(Logging.LEVEL_WARN, "RemoteControl: invalid selection '"+i+"' ignored.", e);
                }
            }
        }
    }
}
