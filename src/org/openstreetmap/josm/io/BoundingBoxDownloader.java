// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.DataSource;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.notes.Note;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.xml.sax.SAXException;

/**
 * Read content from OSM server for a given bounding box
 * @since 627
 */
public class BoundingBoxDownloader extends OsmServerReader {

    /**
     * The boundings of the desired map data.
     */
    protected final double lat1;
    protected final double lon1;
    protected final double lat2;
    protected final double lon2;
    protected final boolean crosses180th;

    /**
     * Constructs a new {@code BoundingBoxDownloader}.
     * @param downloadArea The area to download
     */
    public BoundingBoxDownloader(Bounds downloadArea) {
        CheckParameterUtil.ensureParameterNotNull(downloadArea, "downloadArea");
        this.lat1 = downloadArea.getMinLat();
        this.lon1 = downloadArea.getMinLon();
        this.lat2 = downloadArea.getMaxLat();
        this.lon2 = downloadArea.getMaxLon();
        this.crosses180th = downloadArea.crosses180thMeridian();
    }

    private GpxData downloadRawGps(Bounds b, ProgressMonitor progressMonitor) throws IOException, OsmTransferException, SAXException {
        boolean done = false;
        GpxData result = null;
        String url = "trackpoints?bbox="+b.getMinLon()+","+b.getMinLat()+","+b.getMaxLon()+","+b.getMaxLat()+"&page=";
        for (int i = 0;!done;++i) {
            progressMonitor.subTask(tr("Downloading points {0} to {1}...", i * 5000, ((i + 1) * 5000)));
            try (InputStream in = getInputStream(url+i, progressMonitor.createSubTaskMonitor(1, true))) {
                if (in == null) {
                    break;
                }
                progressMonitor.setTicks(0);
                GpxReader reader = new GpxReader(in);
                gpxParsedProperly = reader.parse(false);
                GpxData currentGpx = reader.getGpxData();
                if (result == null) {
                    result = currentGpx;
                } else if (currentGpx.hasTrackPoints()) {
                    result.mergeFrom(currentGpx);
                } else{
                    done = true;
                }
            }
            activeConnection = null;
        }
        if (result != null) {
            result.fromServer = true;
            result.dataSources.add(new DataSource(b, "OpenStreetMap server"));
        }
        return result;
    }

    @Override
    public GpxData parseRawGps(ProgressMonitor progressMonitor) throws OsmTransferException {
        progressMonitor.beginTask("", 1);
        try {
            progressMonitor.indeterminateSubTask(tr("Contacting OSM Server..."));
            if (crosses180th) {
                // API 0.6 does not support requests crossing the 180th meridian, so make two requests
                GpxData result = downloadRawGps(new Bounds(lat1, lon1, lat2, 180.0), progressMonitor);
                result.mergeFrom(downloadRawGps(new Bounds(lat1, -180.0, lat2, lon2), progressMonitor));
                return result;
            } else {
                // Simple request
                return downloadRawGps(new Bounds(lat1, lon1, lat2, lon2), progressMonitor);
            }
        } catch (IllegalArgumentException e) {
            // caused by HttpUrlConnection in case of illegal stuff in the response
            if (cancel)
                return null;
            throw new OsmTransferException("Illegal characters within the HTTP-header response.", e);
        } catch (IOException e) {
            if (cancel)
                return null;
            throw new OsmTransferException(e);
        } catch (SAXException e) {
            throw new OsmTransferException(e);
        } catch (OsmTransferException e) {
            throw e;
        } catch (RuntimeException e) {
            if (cancel)
                return null;
            throw e;
        } finally {
            progressMonitor.finishTask();
        }
    }

    protected String getRequestForBbox(double lon1, double lat1, double lon2, double lat2) {
        return "map?bbox=" + lon1 + "," + lat1 + "," + lon2 + "," + lat2;
    }

    @Override
    public DataSet parseOsm(ProgressMonitor progressMonitor) throws OsmTransferException {
        progressMonitor.beginTask(tr("Contacting OSM Server..."), 10);
        try {
            DataSet ds = null;
            progressMonitor.indeterminateSubTask(null);
            if (crosses180th) {
                // API 0.6 does not support requests crossing the 180th meridian, so make two requests
                DataSet ds2 = null;

                try (InputStream in = getInputStream(getRequestForBbox(lon1, lat1, 180.0, lat2), progressMonitor.createSubTaskMonitor(9, false))) {
                    if (in == null)
                        return null;
                    ds = OsmReader.parseDataSet(in, progressMonitor.createSubTaskMonitor(1, false));
                }

                try (InputStream in = getInputStream(getRequestForBbox(-180.0, lat1, lon2, lat2), progressMonitor.createSubTaskMonitor(9, false))) {
                    if (in == null)
                        return null;
                    ds2 = OsmReader.parseDataSet(in, progressMonitor.createSubTaskMonitor(1, false));
                }
                if (ds2 == null)
                    return null;
                ds.mergeFrom(ds2);

            } else {
                // Simple request
                try (InputStream in = getInputStream(getRequestForBbox(lon1, lat1, lon2, lat2), progressMonitor.createSubTaskMonitor(9, false))) {
                    if (in == null)
                        return null;
                    ds = OsmReader.parseDataSet(in, progressMonitor.createSubTaskMonitor(1, false));
                }
            }
            return ds;
        } catch(OsmTransferException e) {
            throw e;
        } catch (Exception e) {
            throw new OsmTransferException(e);
        } finally {
            progressMonitor.finishTask();
            activeConnection = null;
        }
    }

    @Override
    public List<Note> parseNotes(Integer noteLimit, Integer daysClosed, ProgressMonitor progressMonitor) throws OsmTransferException {
        progressMonitor.beginTask("Downloading notes");
        noteLimit = checkNoteLimit(noteLimit);
        daysClosed = checkDaysClosed(daysClosed);
        String url = new StringBuilder()
        .append("notes?limit=")
        .append(noteLimit)
        .append("&closed=")
        .append(daysClosed)
        .append("&bbox=")
        .append(lon1)
        .append(",").append(lat1)
        .append(",").append(lon2)
        .append(",").append(lat2)
        .toString();
        try {
            InputStream is = getInputStream(url, progressMonitor.createSubTaskMonitor(1, false));
            NoteReader reader = new NoteReader(is);
            return reader.parse();
        } catch (IOException e) {
            throw new OsmTransferException(e);
        } catch (SAXException e) {
            throw new OsmTransferException(e);
        } finally {
            progressMonitor.finishTask();
        }
    }

    private Integer checkNoteLimit(Integer limit) {
        if (limit == null) {
            limit = Main.pref.getInteger("osm.notes.downloadLimit", 1000);
        }
        if (limit > 10000) {
            Main.error("Requested note limit is over API hard limit of 10000. Reducing to 10000.");
            limit = 10000;
        }
        if (limit < 1) {
            Main.error("Requested note limit is less than 1. Setting to 1.");
            limit = 1;
        }
        Main.debug("returning note limit: " + limit);
        return limit;
    }

    private Integer checkDaysClosed(Integer days) {
        if (days == null) {
            days = Main.pref.getInteger("osm.notes.daysClosed", 1);
        }
        if (days < -1) {
            Main.error("Requested days closed must be greater than -1");
            days = -1;
        }
        Main.debug("returning days closed: " + days);
        return days;
    }

}
