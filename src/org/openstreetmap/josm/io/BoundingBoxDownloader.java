// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.xml.sax.SAXException;


public class BoundingBoxDownloader extends OsmServerReader {

    /**
     * The boundings of the desired map data.
     */
    private final double lat1;
    private final double lon1;
    private final double lat2;
    private final double lon2;

    public BoundingBoxDownloader(Bounds downloadArea) {
        this.lat1 = downloadArea.getMin().lat();
        this.lon1 = downloadArea.getMin().lon();
        this.lat2 = downloadArea.getMax().lat();
        this.lon2 = downloadArea.getMax().lon();
    }

    /**
     * Retrieve raw gps waypoints from the server API.
     * @return A list of all primitives retrieved. Currently, the list of lists
     *      contain only one list, since the server cannot distinguish between
     *      ways.
     */
    public GpxData parseRawGps(ProgressMonitor progressMonitor) throws IOException, SAXException,OsmTransferException {
        progressMonitor.beginTask("", 1);
        try {
            progressMonitor.indeterminateSubTask(tr("Contacting OSM Server..."));
            String url = "trackpoints?bbox="+lon1+","+lat1+","+lon2+","+lat2+"&page=";            

            boolean done = false;
            GpxData result = null;
            for (int i = 0;!done;++i) {
                progressMonitor.subTask(tr("Downloading points {0} to {1}...", i * 5000, ((i + 1) * 5000)));
                InputStream in = getInputStream(url+i, progressMonitor.createSubTaskMonitor(1, true));
                if (in == null) {
                    break;
                }
                progressMonitor.setTicks(0);
                GpxData currentGpx = new GpxReader(in, null).data;
                if (result == null) {
                    result = currentGpx;
                } else if (currentGpx.hasTrackPoints()) {
                    result.mergeFrom(currentGpx);
                } else{
                    done = true;
                }
                in.close();
                activeConnection = null;
            }
            result.fromServer = true;
            return result;
        } catch (IllegalArgumentException e) {
            // caused by HttpUrlConnection in case of illegal stuff in the response
            if (cancel)
                return null;
            throw new SAXException("Illegal characters within the HTTP-header response.", e);
        } catch (IOException e) {
            if (cancel)
                return null;
            throw e;
        } catch (SAXException e) {
            throw e;
        } catch (OsmTransferException e) {
            throw e;
        } catch (Exception e) {
            if (cancel)
                return null;
            if (e instanceof RuntimeException)
                throw (RuntimeException)e;
            throw new RuntimeException(e);
        } finally {
            progressMonitor.finishTask();
        }
    }

    /**
     * Read the data from the osm server address.
     * @return A data set containing all data retrieved from that url
     */
    @Override
    public DataSet parseOsm(ProgressMonitor progressMonitor) throws OsmTransferException {
        progressMonitor.beginTask(tr("Contacting OSM Server..."), 10);
        InputStream in = null;
        try {
            progressMonitor.indeterminateSubTask(null);
            in = getInputStream("map?bbox="+lon1+","+lat1+","+lon2+","+lat2, progressMonitor.createSubTaskMonitor(9, false));
            if (in == null)
                return null;
            return OsmReader.parseDataSet(in, progressMonitor.createSubTaskMonitor(1, false));
        } catch(OsmTransferException e) {
            throw e;
        } catch (Exception e) {
            throw new OsmTransferException(e);
        } finally {
            progressMonitor.finishTask();
            if (in != null) {
                try {in.close();} catch(IOException e) {}
            }
            activeConnection = null;
        }
    }
}
