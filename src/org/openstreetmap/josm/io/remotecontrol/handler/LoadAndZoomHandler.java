// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol.handler;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.actions.AutoScaleAction.AutoScaleMode;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadParams;
import org.openstreetmap.josm.actions.downloadtasks.PostDownloadHandler;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.data.osm.search.SearchCompiler;
import org.openstreetmap.josm.data.osm.search.SearchParseError;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.OsmApiException;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.io.remotecontrol.AddTagsDialog;
import org.openstreetmap.josm.io.remotecontrol.PermissionPrefWithDefault;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.SubclassFilteredCollection;
import org.openstreetmap.josm.tools.Utils;

/**
 * Handler for {@code load_and_zoom} and {@code zoom} requests.
 * @since 3707
 */
public class LoadAndZoomHandler extends RequestHandler {

    /**
     * The remote control command name used to load data and zoom.
     */
    public static final String command = "load_and_zoom";

    /**
     * The remote control command name used to zoom.
     */
    public static final String command2 = "zoom";
    private static final String CURRENT_SELECTION = "currentselection";
    private static final String SELECT = "select";
    private static final String ADDTAGS = "addtags";
    private static final String CHANGESET_COMMENT = "changeset_comment";
    private static final String CHANGESET_SOURCE = "changeset_source";
    private static final String CHANGESET_HASHTAGS = "changeset_hashtags";
    private static final String CHANGESET_TAGS = "changeset_tags";
    private static final String SEARCH = "search";

    // Mandatory arguments
    private double minlat;
    private double maxlat;
    private double minlon;
    private double maxlon;

    // Optional argument 'select'
    private final Set<SimplePrimitiveId> toSelect = new LinkedHashSet<>();

    private boolean isKeepingCurrentSelection;

    @Override
    public String getPermissionMessage() {
        String msg = tr("Remote Control has been asked to load data from the API.") +
                "<br>" + tr("Bounding box: ") + new BBox(minlon, minlat, maxlon, maxlat).toStringCSV(", ");
        if (args.containsKey(SELECT) && !toSelect.isEmpty()) {
            msg += "<br>" + tr("Selection: {0}", toSelect.size());
        }
        return msg;
    }

    @Override
    public String[] getMandatoryParams() {
        return new String[] {"bottom", "top", "left", "right"};
    }

    @Override
    public String[] getOptionalParams() {
        return new String[] {"new_layer", "layer_name", ADDTAGS, SELECT, "zoom_mode",
                CHANGESET_COMMENT, CHANGESET_SOURCE, CHANGESET_HASHTAGS, CHANGESET_TAGS,
                SEARCH, "layer_locked", "download_policy", "upload_policy"};
    }

    @Override
    public String getUsage() {
        return "download a bounding box from the API, zoom to the downloaded area and optionally select one or more objects";
    }

    @Override
    public String[] getUsageExamples() {
        return getUsageExamples(myCommand);
    }

    @Override
    public String[] getUsageExamples(String cmd) {
        if (command.equals(cmd)) {
            return new String[] {
                    "/load_and_zoom?addtags=wikipedia:de=Wei%C3%9Fe_Gasse|maxspeed=5&select=way23071688,way23076176,way23076177," +
                            "&left=13.740&right=13.741&top=51.05&bottom=51.049",
                    "/load_and_zoom?left=8.19&right=8.20&top=48.605&bottom=48.590&select=node413602999&new_layer=true"};
        } else {
            return new String[] {
            "/zoom?left=8.19&right=8.20&top=48.605&bottom=48.590&select=node413602999",
            "/zoom?left=8.19&right=8.20&top=48.605&bottom=48.590&search=highway+OR+railway",
            "/zoom?left=8.19&right=8.20&top=48.605&bottom=48.590&search=" + CURRENT_SELECTION + "&addtags=foo=bar",
            };
        }
    }

    @Override
    protected void handleRequest() throws RequestHandlerErrorException {
        download();
        /*
         * deselect objects if parameter addtags given
         */
        if (args.containsKey(ADDTAGS) && !isKeepingCurrentSelection) {
            GuiHelper.executeByMainWorkerInEDT(() -> {
                DataSet ds = MainApplication.getLayerManager().getEditDataSet();
                if (ds == null) // e.g. download failed
                    return;
                ds.clearSelection();
            });
        }

        Collection<OsmPrimitive> forTagAdd = performSearchZoom();

        // This comes before the other changeset tags, so that they can be overridden
        parseChangesetTags(args);

        // add changeset tags after download if necessary
        addChangesetTags();

        // add tags to objects
        addTags(forTagAdd);
    }

    private void download() throws RequestHandlerErrorException {
        DownloadOsmTask osmTask = new DownloadOsmTask();
        try {
            final DownloadParams settings = getDownloadParams();

            if (command.equals(myCommand)) {
                if (!PermissionPrefWithDefault.LOAD_DATA.isAllowed()) {
                    Logging.info("RemoteControl: download forbidden by preferences");
                } else {
                    Area toDownload = null;
                    if (!settings.isNewLayer()) {
                        toDownload = removeAlreadyDownloadedArea();
                    }
                    if (toDownload != null && toDownload.isEmpty()) {
                        Logging.info("RemoteControl: no download necessary");
                    } else {
                        performDownload(osmTask, settings);
                    }
                }
            }
        } catch (RuntimeException ex) { // NOPMD
            Logging.warn("RemoteControl: Error parsing load_and_zoom remote control request:");
            Logging.error(ex);
            throw new RequestHandlerErrorException(ex);
        }
    }

    /**
     * Remove areas that has already been downloaded
     * @return The area to download
     */
    private Area removeAlreadyDownloadedArea() {
        // find out whether some data has already been downloaded
        Area toDownload = null;
        Area present = null;
        DataSet ds = MainApplication.getLayerManager().getEditDataSet();
        if (ds != null) {
            present = ds.getDataSourceArea();
        }
        if (present != null && !present.isEmpty()) {
            toDownload = new Area(new Rectangle2D.Double(minlon, minlat, maxlon-minlon, maxlat-minlat));
            toDownload.subtract(present);
            if (!toDownload.isEmpty()) {
                // the result might not be a rectangle (L shaped etc)
                Rectangle2D downloadBounds = toDownload.getBounds2D();
                minlat = downloadBounds.getMinY();
                minlon = downloadBounds.getMinX();
                maxlat = downloadBounds.getMaxY();
                maxlon = downloadBounds.getMaxX();
            }
        }
        return toDownload;
    }

    /**
     * Perform the actual download; this is synchronized to ensure that we only have one download going on at a time
     * @param osmTask The task that will show a dialog
     * @param settings The download settings
     * @throws RequestHandlerErrorException If there is an issue getting data
     */
    private void performDownload(DownloadOsmTask osmTask, DownloadParams settings) throws RequestHandlerErrorException {
        Future<?> future = MainApplication.worker.submit(
                new PostDownloadHandler(osmTask, osmTask.download(settings, new Bounds(minlat, minlon, maxlat, maxlon),
                        null /* let the task manage the progress monitor */)));
        GuiHelper.executeByMainWorkerInEDT(() -> {
            try {
                future.get(OSM_DOWNLOAD_TIMEOUT.get(), TimeUnit.SECONDS);
                if (osmTask.isFailed()) {
                    Object error = osmTask.getErrorObjects().get(0);
                    if (error instanceof OsmApiException) {
                        throw (OsmApiException) error;
                    }
                    List<Throwable> exceptions = osmTask.getErrorObjects().stream()
                            .filter(Throwable.class::isInstance).map(Throwable.class::cast)
                            .collect(Collectors.toList());
                    OsmTransferException osmTransferException =
                            new OsmTransferException(String.join(", ", osmTask.getErrorMessages()));
                    if (!exceptions.isEmpty()) {
                        osmTransferException.initCause(exceptions.get(0));
                        exceptions.remove(0);
                        exceptions.forEach(osmTransferException::addSuppressed);
                    }
                    throw osmTransferException;
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                ExceptionDialogUtil.explainException(ex);
            } catch (ExecutionException | TimeoutException |
                     OsmTransferException | RuntimeException ex) { // NOPMD
                ExceptionDialogUtil.explainException(ex);
            }
        });
        // Don't block forever, but do wait some period of time.
        try {
            future.get(OSM_DOWNLOAD_TIMEOUT.get(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RequestHandlerErrorException(e);
        } catch (TimeoutException | ExecutionException e) {
            throw new RequestHandlerErrorException(e);
        }
    }

    private Collection<OsmPrimitive> performSearchZoom() throws RequestHandlerErrorException {
        final Collection<OsmPrimitive> forTagAdd = new LinkedHashSet<>();
        final Bounds bbox = new Bounds(minlat, minlon, maxlat, maxlon);
        if (args.containsKey(SELECT) && PermissionPrefWithDefault.CHANGE_SELECTION.isAllowed()) {
            // select objects after downloading, zoom to selection.
            GuiHelper.executeByMainWorkerInEDT(() -> selectAndZoom(forTagAdd, bbox));
        } else if (args.containsKey(SEARCH) && PermissionPrefWithDefault.CHANGE_SELECTION.isAllowed()) {
            searchAndZoom(forTagAdd, bbox);
        } else {
            // after downloading, zoom to downloaded area.
            zoom(Collections.emptySet(), bbox);
        }
        return forTagAdd;
    }

    private void selectAndZoom(Collection<OsmPrimitive> forTagAdd, Bounds bbox) {
        Set<OsmPrimitive> newSel = new LinkedHashSet<>();
        DataSet ds = MainApplication.getLayerManager().getEditDataSet();
        if (ds == null) // e.g. download failed
            return;
        for (SimplePrimitiveId id : toSelect) {
            final OsmPrimitive p = ds.getPrimitiveById(id);
            if (p != null) {
                newSel.add(p);
                forTagAdd.add(p);
            }
        }
        if (isKeepingCurrentSelection) {
            Collection<OsmPrimitive> sel = ds.getSelected();
            newSel.addAll(sel);
            forTagAdd.addAll(sel);
        }
        toSelect.clear();
        ds.setSelected(newSel);
        zoom(newSel, bbox);
        MapFrame map = MainApplication.getMap();
        if (MainApplication.isDisplayingMapView() && map.relationListDialog != null) {
            map.relationListDialog.selectRelations(null); // unselect all relations to fix #7342
            map.relationListDialog.dataChanged(null);
            map.relationListDialog.selectRelations(Utils.filteredCollection(newSel, Relation.class));
        }
    }

    private void searchAndZoom(Collection<OsmPrimitive> forTagAdd, Bounds bbox) throws RequestHandlerErrorException {
        try {
            final SearchCompiler.Match search = SearchCompiler.compile(args.get(SEARCH));
            MainApplication.worker.submit(() -> {
                final DataSet ds = MainApplication.getLayerManager().getEditDataSet();
                final Collection<OsmPrimitive> filteredPrimitives = SubclassFilteredCollection.filter(ds.allPrimitives(), search);
                ds.setSelected(filteredPrimitives);
                forTagAdd.addAll(filteredPrimitives);
                zoom(filteredPrimitives, bbox);
            });
        } catch (SearchParseError ex) {
            Logging.error(ex);
            throw new RequestHandlerErrorException(ex);
        }
    }

    private void addChangesetTags() {
        List<String> values = Stream.of(CHANGESET_COMMENT, CHANGESET_SOURCE, CHANGESET_HASHTAGS)
                .filter(args::containsKey).collect(Collectors.toList());
        if (values.isEmpty()) {
            return;
        }
        MainApplication.worker.submit(() -> {
            DataSet ds = MainApplication.getLayerManager().getEditDataSet();
            if (ds != null) {
                for (String tag : values) {
                    final String tagKey = tag.substring("changeset_".length());
                    final String value = args.get(tag);
                    if (!Utils.isStripEmpty(value)) {
                        ds.addChangeSetTag(tagKey, value);
                    } else {
                        ds.addChangeSetTag(tagKey, null);
                    }
                }
            }
        });
    }

    private void addTags(Collection<OsmPrimitive> forTagAdd) {
        if (args.containsKey(ADDTAGS)) {
            // needs to run in EDT since forTagAdd is updated in EDT as well
            GuiHelper.executeByMainWorkerInEDT(() -> {
                if (!forTagAdd.isEmpty()) {
                    AddTagsDialog.addTags(args, sender, forTagAdd);
                } else {
                    new Notification(isKeepingCurrentSelection
                            ? tr("You clicked on a JOSM remotecontrol link that would apply tags onto selected objects.\n"
                            + "Since no objects have been selected before this click, no tags were added.\n"
                            + "Select one or more objects and click the link again.")
                            : tr("You clicked on a JOSM remotecontrol link that would apply tags onto objects.\n"
                            + "Unfortunately that link seems to be broken.\n"
                            + "Technical explanation: the URL query parameter ''select='' or ''search='' has an invalid value.\n"
                            + "Ask someone at the origin of the clicked link to fix this.")
                    ).setIcon(JOptionPane.WARNING_MESSAGE).setDuration(Notification.TIME_LONG).show();
                }
            });
        }
    }

    static void parseChangesetTags(Map<String, String> args) {
        if (args.containsKey(CHANGESET_TAGS)) {
            MainApplication.worker.submit(() -> {
                DataSet ds = MainApplication.getLayerManager().getEditDataSet();
                if (ds != null) {
                    AddTagsDialog.parseUrlTagsToKeyValues(args.get(CHANGESET_TAGS)).forEach(ds::addChangeSetTag);
                }
            });
        }
    }

    protected void zoom(Collection<OsmPrimitive> primitives, final Bounds bbox) {
        if (!PermissionPrefWithDefault.CHANGE_VIEWPORT.isAllowed()) {
            return;
        }
        // zoom_mode=(download|selection), defaults to selection
        if (!"download".equals(args.get("zoom_mode")) && !primitives.isEmpty()) {
            AutoScaleAction.autoScale(AutoScaleMode.SELECTION);
        } else if (MainApplication.isDisplayingMapView()) {
            // make sure this isn't called unless there *is* a MapView
            GuiHelper.executeByMainWorkerInEDT(() -> {
                BoundingXYVisitor bbox1 = new BoundingXYVisitor();
                bbox1.visit(bbox);
                MainApplication.getMap().mapView.zoomTo(bbox1);
            });
        }
    }

    @Override
    public PermissionPrefWithDefault getPermissionPref() {
        return null;
    }

    @Override
    protected void validateRequest() throws RequestHandlerBadRequestException {
        validateDownloadParams();
        // Process mandatory arguments
        minlat = 0;
        maxlat = 0;
        minlon = 0;
        maxlon = 0;
        try {
            minlat = LatLon.roundToOsmPrecision(Double.parseDouble(args != null ? args.get("bottom") : ""));
            maxlat = LatLon.roundToOsmPrecision(Double.parseDouble(args != null ? args.get("top") : ""));
            minlon = LatLon.roundToOsmPrecision(Double.parseDouble(args != null ? args.get("left") : ""));
            maxlon = LatLon.roundToOsmPrecision(Double.parseDouble(args != null ? args.get("right") : ""));
        } catch (NumberFormatException e) {
            throw new RequestHandlerBadRequestException("NumberFormatException ("+e.getMessage()+')', e);
        }

        // Current API 0.6 check: "The latitudes must be between -90 and 90"
        if (!LatLon.isValidLat(minlat) || !LatLon.isValidLat(maxlat)) {
            throw new RequestHandlerBadRequestException(tr("The latitudes must be between {0} and {1}", -90d, 90d));
        }
        // Current API 0.6 check: "longitudes between -180 and 180"
        if (!LatLon.isValidLon(minlon) || !LatLon.isValidLon(maxlon)) {
            throw new RequestHandlerBadRequestException(tr("The longitudes must be between {0} and {1}", -180d, 180d));
        }
        // Current API 0.6 check: "the minima must be less than the maxima"
        if (minlat > maxlat || minlon > maxlon) {
            throw new RequestHandlerBadRequestException(tr("The minima must be less than the maxima"));
        }

        // Process optional argument 'select'
        validateSelect();
    }

    private void validateSelect() {
        if (args != null && args.containsKey(SELECT)) {
            toSelect.clear();
            for (String item : args.get(SELECT).split(",", -1)) {
                if (!item.isEmpty()) {
                    if (CURRENT_SELECTION.equalsIgnoreCase(item)) {
                        isKeepingCurrentSelection = true;
                        continue;
                    }
                    try {
                        toSelect.add(SimplePrimitiveId.fromString(item));
                    } catch (IllegalArgumentException ex) {
                        Logging.log(Logging.LEVEL_WARN, "RemoteControl: invalid selection '" + item + "' ignored", ex);
                    }
                }
            }
        }
    }
}
