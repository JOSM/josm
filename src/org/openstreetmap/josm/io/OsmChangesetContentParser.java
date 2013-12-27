// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.ChangesetDataSet;
import org.openstreetmap.josm.data.osm.ChangesetDataSet.ChangesetModificationType;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Utils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Parser for OSM changeset content.
 * @since 2688
 */
public class OsmChangesetContentParser {

    private InputSource source;
    private final ChangesetDataSet data = new ChangesetDataSet();

    private class Parser extends AbstractParser {

        /** the current change modification type */
        private ChangesetDataSet.ChangesetModificationType currentModificationType;

        protected void throwException(String message) throws OsmDataParsingException {
            throw new OsmDataParsingException(message).rememberLocation(locator);
        }

        protected void throwException(Exception e) throws OsmDataParsingException {
            throw new OsmDataParsingException(e).rememberLocation(locator);
        }

        @Override public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            if (super.doStartElement(qName, atts)) {
                // done
            } else if (qName.equals("osmChange")) {
                // do nothing
            } else if (qName.equals("create")) {
                currentModificationType = ChangesetModificationType.CREATED;
            } else if (qName.equals("modify")) {
                currentModificationType = ChangesetModificationType.UPDATED;
            } else if (qName.equals("delete")) {
                currentModificationType = ChangesetModificationType.DELETED;
            } else {
                Main.warn(tr("Unsupported start element ''{0}'' in changeset content at position ({1},{2}). Skipping.", qName, locator.getLineNumber(), locator.getColumnNumber()));
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (qName.equals("node")
                    || qName.equals("way")
                    || qName.equals("relation")) {
                if (currentModificationType == null) {
                    throwException(tr("Illegal document structure. Found node, way, or relation outside of ''create'', ''modify'', or ''delete''."));
                }
                data.put(currentPrimitive, currentModificationType);
            } else if (qName.equals("osmChange")) {
                // do nothing
            } else if (qName.equals("create")) {
                currentModificationType = null;
            } else if (qName.equals("modify")) {
                currentModificationType = null;
            } else if (qName.equals("delete")) {
                currentModificationType = null;
            } else if (qName.equals("tag")) {
                // do nothing
            } else if (qName.equals("nd")) {
                // do nothing
            } else if (qName.equals("member")) {
                // do nothing
            } else {
                Main.warn(tr("Unsupported end element ''{0}'' in changeset content at position ({1},{2}). Skipping.", qName, locator.getLineNumber(), locator.getColumnNumber()));
            }
        }

        @Override
        public void error(SAXParseException e) throws SAXException {
            throwException(e);
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            throwException(e);
        }
    }

    /**
     * Constructs a new {@code OsmChangesetContentParser}.
     *
     * @param source the input stream with the changeset content as XML document. Must not be null.
     * @throws IllegalArgumentException if source is {@code null}.
     */
    public OsmChangesetContentParser(InputStream source) {
        CheckParameterUtil.ensureParameterNotNull(source, "source");
        this.source = new InputSource(new InputStreamReader(source, Utils.UTF_8));
    }

    /**
     * Constructs a new {@code OsmChangesetContentParser}.
     *
     * @param source the input stream with the changeset content as XML document. Must not be null.
     * @throws IllegalArgumentException if source is {@code null}.
     */
    public OsmChangesetContentParser(String source) {
        CheckParameterUtil.ensureParameterNotNull(source, "source");
        this.source = new InputSource(new StringReader(source));
    }

    /**
     * Parses the content.
     *
     * @param progressMonitor the progress monitor. Set to {@link NullProgressMonitor#INSTANCE} if null
     * @return the parsed data
     * @throws OsmDataParsingException thrown if something went wrong. Check for chained
     * exceptions.
     */
    public ChangesetDataSet parse(ProgressMonitor progressMonitor) throws OsmDataParsingException {
        if (progressMonitor == null) {
            progressMonitor = NullProgressMonitor.INSTANCE;
        }
        try {
            progressMonitor.beginTask("");
            progressMonitor.indeterminateSubTask(tr("Parsing changeset content ..."));
            SAXParserFactory.newInstance().newSAXParser().parse(source, new Parser());
        } catch(OsmDataParsingException e){
            throw e;
        } catch (ParserConfigurationException e) {
            throw new OsmDataParsingException(e);
        } catch(SAXException e) {
            throw new OsmDataParsingException(e);
        } catch(IOException e) {
            throw new OsmDataParsingException(e);
        } finally {
            progressMonitor.finishTask();
        }
        return data;
    }

    /**
     * Parses the content from the input source
     *
     * @return the parsed data
     * @throws OsmDataParsingException thrown if something went wrong. Check for chained
     * exceptions.
     */
    public ChangesetDataSet parse() throws OsmDataParsingException {
        return parse(null);
    }
}
