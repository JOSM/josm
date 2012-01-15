// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol.handler;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.concurrent.Future;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadTask;
import org.openstreetmap.josm.actions.downloadtasks.PostDownloadHandler;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.io.remotecontrol.AddTagsDialog;
import org.openstreetmap.josm.tools.Utils;

/**
 * Handler for load_and_zoom request.
 */
public class LoadAndZoomHandler extends RequestHandler
{
    public static final String command = "load_and_zoom";
    public static final String command2 = "zoom";

    public static final String loadDataPermissionKey = "remotecontrol.permission.load-data";
    public static final boolean loadDataPermissionDefault = true;
    public static final String changeSelectionPermissionKey = "remotecontrol.permission.change-selection";
    public static final boolean changeSelectionPermissionDefault = true;
    public static final String changeViewportPermissionKey = "remotecontrol.permission.change-viewport";
    public static final boolean changeViewportPermissionDefault = true;

    @Override
    public String getPermissionMessage()
    {
        return tr("Remote Control has been asked to load data from the API.") +
                "<br>" + tr("Request details: {0}", request);
    }

    @Override
    protected String[] getMandatoryParams()
    {
        return new String[] { "bottom", "top", "left", "right" };
    }

    @Override
    protected void handleRequest() throws RequestHandlerErrorException
    {
        DownloadTask osmTask = new DownloadOsmTask();
        double minlat = 0;
        double maxlat = 0;
        double minlon = 0;
        double maxlon = 0;
        try {
            minlat = Double.parseDouble(args.get("bottom"));
            maxlat = Double.parseDouble(args.get("top"));
            minlon = Double.parseDouble(args.get("left"));
            maxlon = Double.parseDouble(args.get("right"));
            boolean newLayer = Boolean.parseBoolean(args.get("new_layer"));

            if(command.equals(myCommand))
            {
                if (!Main.pref.getBoolean(loadDataPermissionKey, loadDataPermissionDefault))
                {
                    System.out.println("RemoteControl: download forbidden by preferences");
                }
                else
                {

                    // find out whether some data has already been downloaded
                    Area present = null;
                    Area toDownload = null;
                    DataSet ds = Main.main.getCurrentDataSet();
                    if (ds != null) {
                        present = ds.getDataSourceArea();
                    }
                    if (present != null && !present.isEmpty()) {
                        toDownload = new Area(new Rectangle2D.Double(minlon,minlat,maxlon-minlon,maxlat-minlat));
                        toDownload.subtract(present);
                        if (!toDownload.isEmpty())
                        {
                            // the result might not be a rectangle (L shaped etc)
                            Rectangle2D downloadBounds = toDownload.getBounds2D();
                            minlat = downloadBounds.getMinY();
                            minlon = downloadBounds.getMinX();
                            maxlat = downloadBounds.getMaxY();
                            maxlon = downloadBounds.getMaxX();
                        }
                    }
                    if((toDownload != null) && toDownload.isEmpty())
                    {
                        System.out.println("RemoteControl: no download necessary");
                    }
                    else
                    {
                        Future<?> future = osmTask.download(newLayer, new Bounds(minlat,minlon,maxlat,maxlon), null /* let the task manage the progress monitor */);
                        Main.worker.submit(new PostDownloadHandler(osmTask, future));
                    }
                }
            }
        } catch (Exception ex) {
            System.out.println("RemoteControl: Error parsing load_and_zoom remote control request:");
            ex.printStackTrace();
            throw new RequestHandlerErrorException();
        }

        /**
         * deselect objects if parameter addtags given
         */
        if (args.containsKey("addtags")) {
            Main.worker.execute(new Runnable() {
                public void run() {
                    DataSet ds = Main.main.getCurrentDataSet();
                    if(ds == null) // e.g. download failed
                        return;
                    ds.clearSelection();
                }
            });
        }

        if (args.containsKey("select") && Main.pref.getBoolean(changeSelectionPermissionKey, changeSelectionPermissionDefault)) {
            // select objects after downloading, zoom to selection.
            final String selection = args.get("select");
            Main.worker.execute(new Runnable() {
                public void run() {
                    HashSet<Long> ways = new HashSet<Long>();
                    HashSet<Long> nodes = new HashSet<Long>();
                    HashSet<Long> relations = new HashSet<Long>();
                    HashSet<OsmPrimitive> newSel = new HashSet<OsmPrimitive>();
                    for (String item : selection.split(",")) {
                        if (item.startsWith("way")) {
                            ways.add(Long.parseLong(item.substring(3)));
                        } else if (item.startsWith("node")) {
                            nodes.add(Long.parseLong(item.substring(4)));
                        } else if (item.startsWith("relation")) {
                            relations.add(Long.parseLong(item.substring(8)));
                        } else if (item.startsWith("rel")) {
                            relations.add(Long.parseLong(item.substring(3)));
                        } else {
                            System.out.println("RemoteControl: invalid selection '"+item+"' ignored");
                        }
                    }
                    DataSet ds = Main.main.getCurrentDataSet();
                    if(ds == null) // e.g. download failed
                        return;
                    for (Way w : ds.getWays()) {
                        if (ways.contains(w.getId())) {
                            newSel.add(w);
                        }
                    }
                    for (Node n : ds.getNodes()) {
                        if (nodes.contains(n.getId())) {
                            newSel.add(n);
                        }
                    }
                    for (Relation r : ds.getRelations()) {
                        if (relations.contains(r.getId())) {
                            newSel.add(r);
                        }
                    }
                    ds.setSelected(newSel);
                    if (Main.pref.getBoolean(changeViewportPermissionKey, changeViewportPermissionDefault)) {
                        new AutoScaleAction("selection").actionPerformed(null);
                    }
                    if (Main.map != null && Main.map.relationListDialog != null) {
                        Main.map.relationListDialog.dataChanged(null);
                        Main.map.relationListDialog.selectRelations(Utils.filteredCollection(newSel, Relation.class));
                    }
                }
            });
        } else if (Main.pref.getBoolean(changeViewportPermissionKey, changeViewportPermissionDefault)) {
            // after downloading, zoom to downloaded area.
            zoom(minlat, maxlat, minlon, maxlon);
        }

        /*
         * parse addtags parameters
         * Example URL (part):
         * addtags=wikipedia:de%3DResidenzschloss Dresden|name:en%3DDresden Castle
         */
        if (args.containsKey("addtags")) {
            Main.worker.execute(new Runnable() {
                public void run() {
                    String[] tags = null;
                    try {
                        tags = URLDecoder.decode(args.get("addtags"), "UTF-8").split("\\|");
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException();
                    }
                    String[][] keyValue = new String[tags.length][2];
                    for (int i = 0; i<tags.length; i++) {
                        keyValue[i] = tags[i].split("=");

                        keyValue[i][0] = keyValue[i][0];
                        keyValue[i][1] = keyValue[i][1];
                    }

                    new AddTagsDialog(keyValue);
                }
            });
        }

    }

    protected void zoom(double minlat, double maxlat, double minlon, double maxlon) {
        final Bounds bounds = new Bounds(new LatLon(minlat, minlon),
                new LatLon(maxlat, maxlon));

        // make sure this isn't called unless there *is* a MapView
        //
        if (Main.map != null && Main.map.mapView != null) {
            Main.worker.execute(new Runnable() {
                public void run() {
                    BoundingXYVisitor bbox = new BoundingXYVisitor();
                    bbox.visit(bounds);
                    Main.map.mapView.recalculateCenterScale(bbox);
                }
            });
        }
    }
}
