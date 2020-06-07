// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.util.List;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.DataSource;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.notes.Note;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;
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
        final int pointsPerPage = 5000; // see https://wiki.openstreetmap.org/wiki/API_v0.6#GPS_traces
        String url = "trackpoints?bbox="+b.getMinLon()+','+b.getMinLat()+','+b.getMaxLon()+','+b.getMaxLat()+"&page=";
        for (int i = 0; !done && !isCanceled(); ++i) {
            progressMonitor.subTask(tr("Downloading points {0} to {1}...", i * pointsPerPage, (i + 1) * pointsPerPage));
            try (InputStream in = getInputStream(url+i, progressMonitor.createSubTaskMonitor(1, true))) {
                if (in == null) {
                    break;
                }
                progressMonitor.setTicks(0);
                GpxReader reader = new GpxReader(in);
                gpxParsedProperly = reader.parse(false);
                GpxData currentGpx = reader.getGpxData();
                long count = 0;
                if (currentGpx.hasTrackPoints()) {
                    count = currentGpx.getTrackPoints().count();
                }
                if (count < pointsPerPage)
                    done = true;
                Logging.debug("got {0} gpx points", count);
                if (result == null) {
                    result = currentGpx;
                } else {
                    result.mergeFrom(currentGpx);
                }
            } catch (OsmApiException ex) {
                throw ex; // this avoids infinite loop in case of API error such as bad request (ex: bbox too large, see #12853)
            } catch (OsmTransferException | SocketException ex) {
                if (isCanceled()) {
                    final OsmTransferCanceledException canceledException = new OsmTransferCanceledException("Operation canceled");
                    canceledException.initCause(ex);
                    Logging.warn(canceledException);
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
            progressMonitor.indeterminateSubTask(getTaskName());
            if (crosses180th) {
                // API 0.6 does not support requests crossing the 180th meridian, so make two requests
                GpxData result = downloadRawGps(new Bounds(lat1, lon1, lat2, 180.0), progressMonitor);
                if (result != null)
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
        } catch (JosmRuntimeException | IllegalStateException e) {
            if (cancel)
                return null;
            throw e;
        } finally {
            progressMonitor.finishTask();
        }
    }

    /**
     * Returns the name of the download task to be displayed in the {@link ProgressMonitor}.
     * @return task name
     */
    protected String getTaskName() {
        return tr("Contacting OSM Server...");
    }

    /**
     * Builds the request part for the bounding box.
     * @param lon1 left
     * @param lat1 bottom
     * @param lon2 right
     * @param lat2 top
     * @return "map?bbox=left,bottom,right,top"
     */
    protected String getRequestForBbox(double lon1, double lat1, double lon2, double lat2) {
        return "map?bbox=" + lon1 + ',' + lat1 + ',' + lon2 + ',' + lat2;
    }

    /**
     * Parse the given input source and return the dataset.
     * @param source input stream
     * @param progressMonitor progress monitor
     * @return dataset
     * @throws IllegalDataException if an error was found while parsing the OSM data
     *
     * @see OsmReader#parseDataSet(InputStream, ProgressMonitor)
     */
    protected DataSet parseDataSet(InputStream source, ProgressMonitor progressMonitor) throws IllegalDataException {
        return OsmReader.parseDataSet(source, progressMonitor);
    }

    @Override
    public DataSet parseOsm(ProgressMonitor progressMonitor) throws OsmTransferException {
        progressMonitor.beginTask(getTaskName(), 10);
        try {
            DataSet ds = null;
            progressMonitor.indeterminateSubTask(null);
            if (crosses180th) {
                // API 0.6 does not support requests crossing the 180th meridian, so make two requests
                DataSet ds2 = null;

                try (InputStream in = getInputStream(getRequestForBbox(lon1, lat1, 180.0, lat2),
                        progressMonitor.createSubTaskMonitor(9, false))) {
                    if (in == null)
                        return null;
                    ds = parseDataSet(in, progressMonitor.createSubTaskMonitor(1, false));
                }

                try (InputStream in = getInputStream(getRequestForBbox(-180.0, lat1, lon2, lat2),
                        progressMonitor.createSubTaskMonitor(9, false))) {
                    if (in == null)
                        return null;
                    ds2 = parseDataSet(in, progressMonitor.createSubTaskMonitor(1, false));
                }
                if (ds2 == null)
                    return null;
                ds.mergeFrom(ds2);

            } else {
                // Simple request
                try (InputStream in = getInputStream(getRequestForBbox(lon1, lat1, lon2, lat2),
                        progressMonitor.createSubTaskMonitor(9, false))) {
                    if (in == null)
                        return null;
                    ds = parseDataSet(in, progressMonitor.createSubTaskMonitor(1, false));
                }
            }
            return ds;
        } catch (OsmTransferException e) {
            throw e;
        } catch (IllegalDataException | IOException e) {
            throw new OsmTransferException(e);
        } finally {
            progressMonitor.finishTask();
            activeConnection = null;
        }
    }

    @Override
    public List<Note> parseNotes(int noteLimit, int daysClosed, ProgressMonitor progressMonitor) throws OsmTransferException {
        progressMonitor.beginTask(tr("Downloading notes"));
        CheckParameterUtil.ensureThat(noteLimit > 0, "Requested note limit is less than 1.");
        // see result_limit in https://github.com/openstreetmap/openstreetmap-website/blob/master/app/controllers/notes_controller.rb
        CheckParameterUtil.ensureThat(noteLimit <= 10_000, "Requested note limit is over API hard limit of 10000.");
        CheckParameterUtil.ensureThat(daysClosed >= -1, "Requested note limit is less than -1.");
        String url = "notes?limit=" + noteLimit + "&closed=" + daysClosed + "&bbox=" + lon1 + ',' + lat1 + ',' + lon2 + ',' + lat2;
        try (InputStream is = getInputStream(url, progressMonitor.createSubTaskMonitor(1, false))) {
            final List<Note> notes = new NoteReader(is).parse();
            if (notes.size() == noteLimit) {
                throw new MoreNotesException(notes, noteLimit);
            }
            return notes;
        } catch (IOException | SAXException e) {
            throw new OsmTransferException(e);
        } finally {
            progressMonitor.finishTask();
        }
    }

    /**
     * Indicates that the number of fetched notes equals the specified limit. Thus there might be more notes to download.
     */
    public static class MoreNotesException extends RuntimeException {
        /**
         * The downloaded notes
         */
        public final transient List<Note> notes;
        /**
         * The download limit sent to the server.
         */
        public final int limit;

        /**
         * Constructs a {@code MoreNotesException}.
         * @param notes downloaded notes
         * @param limit download limit sent to the server
         */
        public MoreNotesException(List<Note> notes, int limit) {
            this.notes = notes;
            this.limit = limit;
        }
    }

    /**
     * Determines if download is complete for the given bounding box.
     * @return true if download is complete for the given bounding box (not filtered)
     */
    public boolean considerAsFullDownload() {
        return true;
    }

}
