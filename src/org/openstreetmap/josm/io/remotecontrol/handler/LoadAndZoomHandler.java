// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol.handler;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadTask;
import org.openstreetmap.josm.actions.downloadtasks.PostDownloadHandler;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.remotecontrol.AddTagsDialog;
import org.openstreetmap.josm.io.remotecontrol.PermissionPrefWithDefault;
import org.openstreetmap.josm.tools.Utils;

/**
 * Handler for load_and_zoom request.
 */
public class LoadAndZoomHandler extends RequestHandler
{
    /**
     * The remote control command name used to load data and zoom.
     */
    public static final String command = "load_and_zoom";

    /**
     * The remote control command name used to zoom.
     */
    public static final String command2 = "zoom";

    // Mandatory arguments
    private double minlat;
    private double maxlat;
    private double minlon;
    private double maxlon;

    // Optional argument 'select'
    private final Set<Long> ways = new HashSet<Long>();
    private final Set<Long> nodes = new HashSet<Long>();
    private final Set<Long> relations = new HashSet<Long>();

    @Override
    public String getPermissionMessage()
    {
        String msg = tr("Remote Control has been asked to load data from the API.") +
                "<br>" + tr("Bounding box: ") + new BBox(minlon, minlat, maxlon, maxlat).toStringCSV(", ");
        if (args.containsKey("select") && ways.size()+nodes.size()+relations.size() > 0) {
            msg += "<br>" + tr("Sel.: Rel.:{0} / Ways:{1} / Nodes:{2}", relations.size(), ways.size(), nodes.size());
        }
        return msg;
    }

    @Override
    public String[] getMandatoryParams()
    {
        return new String[] { "bottom", "top", "left", "right" };
    }

    @Override
    public String[] getOptionalParams()
    {
        return new String[] {"new_layer", "addtags", "select", "zoom_mode"};
    }

    @Override
    public String[] getUsageExamples() {
        if (command.equals(myCommand)) {
            return new String[] { 
                "/load_and_zoom?addtags=wikipedia:de=Wei%C3%9Fe_Gasse|maxspeed=5&select=way23071688,way23076176,way23076177,&left=13.740&right=13.741&top=51.05&bottom=51.049",
                "/load_and_zoom?left=8.19&right=8.20&top=48.605&bottom=48.590&select=node413602999&new_layer=true"};
        } else {
            return new String[] { 
                "/zoom?left=8.19&right=8.20&top=48.605&bottom=48.590&select=node413602999"};
        }
    }
    
    @Override
    protected void handleRequest() throws RequestHandlerErrorException
    {
        DownloadTask osmTask = new DownloadOsmTask();
        try {
            boolean newLayer = isLoadInNewLayer();

            if(command.equals(myCommand))
            {
                if (!PermissionPrefWithDefault.LOAD_DATA.isAllowed())
                {
                    System.out.println("RemoteControl: download forbidden by preferences");
                }
                else
                {
                    Area toDownload = null;
                    if (!newLayer) {
                        // find out whether some data has already been downloaded
                        Area present = null;
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
                    }
                    if (toDownload != null && toDownload.isEmpty())
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
            GuiHelper.executeByMainWorkerInEDT(new Runnable() {
                @Override
                public void run() {
                    DataSet ds = Main.main.getCurrentDataSet();
                    if(ds == null) // e.g. download failed
                        return;
                    ds.clearSelection();
                }
            });
        }

        final Bounds bbox = new Bounds(new LatLon(minlat, minlon), new LatLon(maxlat, maxlon));
        if (args.containsKey("select") && PermissionPrefWithDefault.CHANGE_SELECTION.isAllowed()) {
            // select objects after downloading, zoom to selection.
            GuiHelper.executeByMainWorkerInEDT(new Runnable() {
                @Override
                public void run() {
                    HashSet<OsmPrimitive> newSel = new HashSet<OsmPrimitive>();
                    DataSet ds = Main.main.getCurrentDataSet();
                    if(ds == null) // e.g. download failed
                        return;
                    for (Way w : ds.getWays()) {
                        if (ways.contains(w.getId())) {
                            newSel.add(w);
                        }
                    }
                    ways.clear();
                    for (Node n : ds.getNodes()) {
                        if (nodes.contains(n.getId())) {
                            newSel.add(n);
                        }
                    }
                    nodes.clear();
                    for (Relation r : ds.getRelations()) {
                        if (relations.contains(r.getId())) {
                            newSel.add(r);
                        }
                    }
                    relations.clear();
                    ds.setSelected(newSel);
                    if (PermissionPrefWithDefault.CHANGE_VIEWPORT.isAllowed()) {
                        // zoom_mode=(download|selection), defaults to selection
                        if (!"download".equals(args.get("zoom_mode")) && !newSel.isEmpty()) {
                            AutoScaleAction.autoScale("selection");
                        } else {
                            zoom(bbox);
                        }
                    }
                    if (Main.isDisplayingMapView() && Main.map.relationListDialog != null) {
                        Main.map.relationListDialog.selectRelations(null); // unselect all relations to fix #7342
                        Main.map.relationListDialog.dataChanged(null);
                        Main.map.relationListDialog.selectRelations(Utils.filteredCollection(newSel, Relation.class));
                    }
                }
            });
        } else if (PermissionPrefWithDefault.CHANGE_VIEWPORT.isAllowed()) {
            // after downloading, zoom to downloaded area.
            zoom(bbox);
        }

        AddTagsDialog.addTags(args, sender);
    }

    protected void zoom(final Bounds bounds) {
        // make sure this isn't called unless there *is* a MapView
        if (Main.isDisplayingMapView()) {
            GuiHelper.executeByMainWorkerInEDT(new Runnable() {
                @Override
                public void run() {
                    BoundingXYVisitor bbox = new BoundingXYVisitor();
                    bbox.visit(bounds);
                    Main.map.mapView.recalculateCenterScale(bbox);
                }
            });
        }
    }

    @Override
    public PermissionPrefWithDefault getPermissionPref() {
        return null;
    }

    @Override
    protected void validateRequest() throws RequestHandlerBadRequestException {
        // Process mandatory arguments
        minlat = 0;
        maxlat = 0;
        minlon = 0;
        maxlon = 0;
        try {
            minlat = LatLon.roundToOsmPrecision(Double.parseDouble(args.get("bottom")));
            maxlat = LatLon.roundToOsmPrecision(Double.parseDouble(args.get("top")));
            minlon = LatLon.roundToOsmPrecision(Double.parseDouble(args.get("left")));
            maxlon = LatLon.roundToOsmPrecision(Double.parseDouble(args.get("right")));
        } catch (NumberFormatException e) {
            throw new RequestHandlerBadRequestException("NumberFormatException ("+e.getMessage()+")");
        }

        // Process optional argument 'select'
        if (args.containsKey("select")) {
            ways.clear();
            nodes.clear();
            relations.clear();
            for (String item : args.get("select").split(",")) {
                try {
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
                } catch (NumberFormatException e) {
                    System.out.println("RemoteControl: invalid selection '"+item+"' ignored");
                }
            }
        }
    }
}
