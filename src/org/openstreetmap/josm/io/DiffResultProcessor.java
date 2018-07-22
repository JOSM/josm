// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.XmlParsingException;
import org.openstreetmap.josm.tools.XmlUtils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Helper class to process the OSM API server response to a "diff" upload.
 * <p>
 * New primitives (uploaded with negative id) will be assigned a positive id, etc.
 * The goal is to have a clean state, just like a fresh download (assuming no
 * concurrent uploads by other users have happened in the meantime).
 * <p>
 * @see <a href="https://wiki.openstreetmap.org/wiki/API_v0.6#Response_10">API 0.6 diff upload response</a>
 */
public class DiffResultProcessor {

    static class DiffResultEntry {
        long newId;
        int newVersion;
    }

    /**
     * mapping from old id to new id and version, the result of parsing the diff result
     * replied by the server
     */
    private final Map<PrimitiveId, DiffResultEntry> diffResults = new HashMap<>();
    /**
     * the set of processed primitives *after* the new id, the new version and the new changeset id is set
     */
    private final Set<OsmPrimitive> processed;
    /**
     * the collection of primitives being uploaded
     */
    private final Collection<? extends OsmPrimitive> primitives;

    /**
     * Creates a diff result reader
     *
     * @param primitives the collection of primitives which have been uploaded. If null,
     * assumes an empty collection.
     */
    public DiffResultProcessor(Collection<? extends OsmPrimitive> primitives) {
        this.primitives = Optional.ofNullable(primitives).orElseGet(Collections::emptyList);
        this.processed = new HashSet<>();
    }

    /**
     * Parse the response from a diff upload to the OSM API.
     *
     * @param diffUploadResponse the response. Must not be null.
     * @param progressMonitor a progress monitor. Defaults to {@link NullProgressMonitor#INSTANCE} if null
     * @throws IllegalArgumentException if diffUploadRequest is null
     * @throws XmlParsingException if the diffUploadRequest can't be parsed successfully
     *
     */
    public void parse(String diffUploadResponse, ProgressMonitor progressMonitor) throws XmlParsingException {
        if (progressMonitor == null) {
            progressMonitor = NullProgressMonitor.INSTANCE;
        }
        CheckParameterUtil.ensureParameterNotNull(diffUploadResponse, "diffUploadResponse");
        try {
            progressMonitor.beginTask(tr("Parsing response from server..."));
            InputSource inputSource = new InputSource(new StringReader(diffUploadResponse));
            XmlUtils.parseSafeSAX(inputSource, new Parser());
        } catch (XmlParsingException e) {
            throw e;
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new XmlParsingException(e);
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
    protected Set<OsmPrimitive> postProcess(Changeset cs, ProgressMonitor monitor) {
        if (monitor == null) {
            monitor = NullProgressMonitor.INSTANCE;
        }
        DataSet ds = null;
        if (!primitives.isEmpty()) {
            ds = primitives.iterator().next().getDataSet();
        }
        boolean readOnly = false;
        if (ds != null) {
            readOnly = ds.isLocked();
            if (readOnly) {
                ds.unlock();
            }
            ds.beginUpdate();
        }
        try {
            monitor.beginTask("Postprocessing uploaded data ...");
            monitor.setTicksCount(primitives.size());
            monitor.setTicks(0);
            for (OsmPrimitive p : primitives) {
                monitor.worked(1);
                DiffResultEntry entry = diffResults.get(p.getPrimitiveId());
                if (entry == null) {
                    continue;
                }
                processed.add(p);
                if (!p.isDeleted()) {
                    p.setOsmId(entry.newId, entry.newVersion);
                    p.setVisible(true);
                } else {
                    p.setVisible(false);
                }
                if (cs != null && !cs.isNew()) {
                    p.setChangesetId(cs.getId());
                    p.setUser(cs.getUser());
                    // TODO is there a way to obtain the timestamp for non-closed changesets?
                    p.setTimestamp(Utils.firstNonNull(cs.getClosedAt(), new Date()));
                }
            }
            return processed;
        } finally {
            if (ds != null) {
                ds.endUpdate();
                if (readOnly) {
                    ds.lock();
                }
            }
            monitor.finishTask();
        }
    }

    private class Parser extends DefaultHandler {
        private Locator locator;

        @Override
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        protected void throwException(String msg) throws XmlParsingException {
            throw new XmlParsingException(msg).rememberLocation(locator);
        }

        @Override
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            try {
                switch (qName) {
                case "diffResult":
                    // the root element, ignore
                    break;
                case "node":
                case "way":
                case "relation":
                    PrimitiveId id = new SimplePrimitiveId(
                            Long.parseLong(atts.getValue("old_id")),
                            OsmPrimitiveType.fromApiTypeName(qName)
                    );
                    DiffResultEntry entry = new DiffResultEntry();
                    if (atts.getValue("new_id") != null) {
                        entry.newId = Long.parseLong(atts.getValue("new_id"));
                    }
                    if (atts.getValue("new_version") != null) {
                        entry.newVersion = Integer.parseInt(atts.getValue("new_version"));
                    }
                    diffResults.put(id, entry);
                    break;
                default:
                    throwException(tr("Unexpected XML element with name ''{0}''", qName));
                }
            } catch (NumberFormatException e) {
                throw new XmlParsingException(e).rememberLocation(locator);
            }
        }
    }

    final Map<PrimitiveId, DiffResultEntry> getDiffResults() {
        return new HashMap<>(diffResults);
    }
}
