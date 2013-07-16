//License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.apache.tools.bzip2.CBZip2InputStream;
import org.openstreetmap.josm.actions.downloadtasks.DownloadGpsTask;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.Utils;
import org.xml.sax.SAXException;

public class OsmServerLocationReader extends OsmServerReader {

    protected final String url;

    public OsmServerLocationReader(String url) {
        this.url = url;
    }

    protected abstract class Parser<T> {
        public InputStream in = null;
        public abstract T parse() throws OsmTransferException, IllegalDataException, IOException, SAXException;
    }

    protected final <T> T doParse(Parser<T> parser, final ProgressMonitor progressMonitor) throws OsmTransferException {
        progressMonitor.beginTask(tr("Contacting Server...", 10));
        try {
            return parser.parse();
        } catch(OsmTransferException e) {
            throw e;
        } catch (Exception e) {
            if (cancel)
                return null;
            throw new OsmTransferException(e);
        } finally {
            progressMonitor.finishTask();
            activeConnection = null;
            Utils.close(parser.in);
            parser.in = null;
        }
    }

    /**
     * Method to download OSM files from somewhere
     */
    @Override
    public DataSet parseOsm(final ProgressMonitor progressMonitor) throws OsmTransferException {
        return doParse(new Parser<DataSet>() {
            @Override
            public DataSet parse() throws OsmTransferException, IllegalDataException {
                in = getInputStreamRaw(url, progressMonitor.createSubTaskMonitor(9, false));
                if (in == null)
                    return null;
                progressMonitor.subTask(tr("Downloading OSM data..."));
                return OsmReader.parseDataSet(in, progressMonitor.createSubTaskMonitor(1, false));
            }
        }, progressMonitor);
    }

    /**
     * Method to download BZip2-compressed OSM files from somewhere
     */
    @Override
    public DataSet parseOsmBzip2(final ProgressMonitor progressMonitor) throws OsmTransferException {
        return doParse(new Parser<DataSet>() {
            @Override
            public DataSet parse() throws OsmTransferException, IllegalDataException, IOException {
                in = getInputStreamRaw(url, progressMonitor.createSubTaskMonitor(9, false));
                if (in == null)
                    return null;
                CBZip2InputStream bzin = FileImporter.getBZip2InputStream(in);
                progressMonitor.subTask(tr("Downloading OSM data..."));
                return OsmReader.parseDataSet(bzin, progressMonitor.createSubTaskMonitor(1, false));
            }
        }, progressMonitor);
    }

    /**
     * Method to download GZip-compressed OSM files from somewhere
     */
    @Override
    public DataSet parseOsmGzip(final ProgressMonitor progressMonitor) throws OsmTransferException {
        return doParse(new Parser<DataSet>() {
            @Override
            public DataSet parse() throws OsmTransferException, IllegalDataException, IOException {
                in = getInputStreamRaw(url, progressMonitor.createSubTaskMonitor(9, false));
                if (in == null)
                    return null;
                GZIPInputStream gzin = FileImporter.getGZipInputStream(in);
                progressMonitor.subTask(tr("Downloading OSM data..."));
                return OsmReader.parseDataSet(gzin, progressMonitor.createSubTaskMonitor(1, false));
            }
        }, progressMonitor);
    }

    /* (non-Javadoc)
     * @see org.openstreetmap.josm.io.OsmServerReader#parseOsmChange(org.openstreetmap.josm.gui.progress.ProgressMonitor)
     */
    @Override
    public DataSet parseOsmChange(final ProgressMonitor progressMonitor)
            throws OsmTransferException {
        return doParse(new Parser<DataSet>() {
            @Override
            public DataSet parse() throws OsmTransferException, IllegalDataException {
                in = getInputStreamRaw(url, progressMonitor.createSubTaskMonitor(9, false));
                if (in == null)
                    return null;
                progressMonitor.subTask(tr("Downloading OSM data..."));
                return OsmChangeReader.parseDataSet(in, progressMonitor.createSubTaskMonitor(1, false));
            }
        }, progressMonitor);
    }

    /**
     * Method to download BZip2-compressed OSM Change files from somewhere
     */
    @Override
    public DataSet parseOsmChangeBzip2(final ProgressMonitor progressMonitor) throws OsmTransferException {
        return doParse(new Parser<DataSet>() {
            @Override
            public DataSet parse() throws OsmTransferException, IllegalDataException, IOException {
                in = getInputStreamRaw(url, progressMonitor.createSubTaskMonitor(9, false));
                if (in == null)
                    return null;
                CBZip2InputStream bzin = FileImporter.getBZip2InputStream(in);
                progressMonitor.subTask(tr("Downloading OSM data..."));
                return OsmChangeReader.parseDataSet(bzin, progressMonitor.createSubTaskMonitor(1, false));
            }
        }, progressMonitor);
    }

    /**
     * Method to download GZip-compressed OSM Change files from somewhere
     */
    @Override
    public DataSet parseOsmChangeGzip(final ProgressMonitor progressMonitor) throws OsmTransferException {
        return doParse(new Parser<DataSet>() {
            @Override
            public DataSet parse() throws OsmTransferException, IllegalDataException, IOException {
                in = getInputStreamRaw(url, progressMonitor.createSubTaskMonitor(9, false));
                if (in == null)
                    return null;
                GZIPInputStream gzin = FileImporter.getGZipInputStream(in);
                progressMonitor.subTask(tr("Downloading OSM data..."));
                return OsmChangeReader.parseDataSet(gzin, progressMonitor.createSubTaskMonitor(1, false));
            }
        }, progressMonitor);
    }

    @Override
    public GpxData parseRawGps(final ProgressMonitor progressMonitor) throws OsmTransferException {
        return doParse(new Parser<GpxData>() {
            @Override
            public GpxData parse() throws OsmTransferException, IllegalDataException, IOException, SAXException {
                in = getInputStreamRaw(url, progressMonitor.createSubTaskMonitor(1, true));
                if (in == null)
                    return null;
                progressMonitor.subTask(tr("Downloading OSM data..."));
                GpxReader reader = new GpxReader(in);
                gpxParsedProperly = reader.parse(false);
                GpxData result = reader.getGpxData();
                result.fromServer = DownloadGpsTask.isFromServer(url);
                return result;
            }
        }, progressMonitor);
    }
}
