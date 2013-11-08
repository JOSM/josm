//License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class DiffResultProcessor  {

    static private class DiffResultEntry {
        public long new_id;
        public int new_version;
    }

    /**
     * mapping from old id to new id and version, the result of parsing the diff result
     * replied by the server
     */
    private Map<PrimitiveId, DiffResultEntry> diffResults = new HashMap<PrimitiveId, DiffResultEntry>();
    /**
     * the set of processed primitives *after* the new id, the new version and the new changeset id
     * is set
     */
    private Set<IPrimitive> processed;
    /**
     * the collection of primitives being uploaded
     */
    private Collection<? extends IPrimitive> primitives;

    /**
     * Creates a diff result reader
     *
     * @param primitives the collection of primitives which have been uploaded. If null,
     * assumes an empty collection.
     */
    public DiffResultProcessor(Collection<? extends IPrimitive> primitives) {
        if (primitives == null) {
            primitives = Collections.emptyList();
        }
        this.primitives = primitives;
        this.processed = new HashSet<IPrimitive>();
    }

    /**
     * Parse the response from a diff upload to the OSM API.
     *
     * @param diffUploadResponse the response. Must not be null.
     * @param progressMonitor a progress monitor. Defaults to {@link NullProgressMonitor#INSTANCE} if null
     * @throws IllegalArgumentException thrown if diffUploadRequest is null
     * @throws OsmDataParsingException thrown if the diffUploadRequest can't be parsed successfully
     *
     */
    public  void parse(String diffUploadResponse, ProgressMonitor progressMonitor) throws OsmDataParsingException {
        if (progressMonitor == null) {
            progressMonitor = NullProgressMonitor.INSTANCE;
        }
        CheckParameterUtil.ensureParameterNotNull(diffUploadResponse, "diffUploadResponse");
        try {
            progressMonitor.beginTask(tr("Parsing response from server..."));
            InputSource inputSource = new InputSource(new StringReader(diffUploadResponse));
            SAXParserFactory.newInstance().newSAXParser().parse(inputSource, new Parser());
        } catch(IOException e) {
            throw new OsmDataParsingException(e);
        } catch(ParserConfigurationException e) {
            throw new OsmDataParsingException(e);
        } catch(OsmDataParsingException e) {
            throw e;
        } catch(SAXException e) {
            throw new OsmDataParsingException(e);
        } finally {
            progressMonitor.finishTask();
        }
    }

    /**
     * Postprocesses the diff result read and parsed from the server.
     *
     * Uploaded objects are assigned their new id (if they got assigned a new
     * id by the server), their new version (if the version was incremented),
     * and the id of the changeset to which they were uploaded.
     *
     * @param cs the current changeset. Ignored if null.
     * @param monitor the progress monitor. Set to {@link NullProgressMonitor#INSTANCE} if null
     * @return the collection of processed primitives
     */
    protected Set<IPrimitive> postProcess(Changeset cs, ProgressMonitor monitor) {
        if (monitor == null) {
            monitor = NullProgressMonitor.INSTANCE;
        }
        try {
            monitor.beginTask("Postprocessing uploaded data ...");
            monitor.setTicksCount(primitives.size());
            monitor.setTicks(0);
            for (IPrimitive p : primitives) {
                monitor.worked(1);
                DiffResultEntry entry = diffResults.get(p.getPrimitiveId());
                if (entry == null) {
                    continue;
                }
                processed.add(p);
                if (!p.isDeleted()) {
                    p.setOsmId(entry.new_id, entry.new_version);
                    p.setVisible(true);
                } else {
                    p.setVisible(false);
                }
                if (cs != null && !cs.isNew()) {
                    p.setChangesetId(cs.getId());
                }
            }
            return processed;
        } finally {
            monitor.finishTask();
        }
    }

    private class Parser extends DefaultHandler {
        private Locator locator;

        @Override
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        protected void throwException(String msg) throws OsmDataParsingException{
            throw new OsmDataParsingException(msg).rememberLocation(locator);
        }

        @Override public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            try {
                if (qName.equals("diffResult")) {
                    // the root element, ignore
                } else if (qName.equals("node") || qName.equals("way") || qName.equals("relation")) {

                    PrimitiveId id  = new SimplePrimitiveId(
                            Long.parseLong(atts.getValue("old_id")),
                            OsmPrimitiveType.fromApiTypeName(qName)
                    );
                    DiffResultEntry entry = new DiffResultEntry();
                    if (atts.getValue("new_id") != null) {
                        entry.new_id = Long.parseLong(atts.getValue("new_id"));
                    }
                    if (atts.getValue("new_version") != null) {
                        entry.new_version = Integer.parseInt(atts.getValue("new_version"));
                    }
                    diffResults.put(id, entry);
                } else {
                    throwException(tr("Unexpected XML element with name ''{0}''", qName));
                }
            } catch (NumberFormatException e) {
                throw new OsmDataParsingException(e).rememberLocation(locator);
            }
        }
    }
}
