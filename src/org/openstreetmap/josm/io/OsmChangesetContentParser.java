// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.ParserConfigurationException;

import org.openstreetmap.josm.data.osm.ChangesetDataSet;
import org.openstreetmap.josm.data.osm.ChangesetDataSet.ChangesetModificationType;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.XmlParsingException;
import org.openstreetmap.josm.tools.XmlUtils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Parser for OSM changeset content.
 * @since 2688
 */
public class OsmChangesetContentParser {

    private final InputSource source;
    private final ChangesetDataSet data = new ChangesetDataSet();

    private class Parser extends AbstractParser {
        Parser(boolean useAnonymousUser) {
            this.useAnonymousUser = useAnonymousUser;
        }

        /** the current change modification type */
        private ChangesetDataSet.ChangesetModificationType currentModificationType;

        @Override
        protected void throwException(String message) throws XmlParsingException {
            throw new XmlParsingException(message).rememberLocation(locator);
        }

        @Override
        protected void throwException(String message, Exception e) throws XmlParsingException {
            throw new XmlParsingException(message, e).rememberLocation(locator);
        }

        @Override
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            if (super.doStartElement(qName, atts)) {
                // done
                return;
            }
            switch (qName) {
            case "osmChange":
                // do nothing
                break;
            case "create":
                currentModificationType = ChangesetModificationType.CREATED;
                break;
            case "modify":
                currentModificationType = ChangesetModificationType.UPDATED;
                break;
            case "delete":
                currentModificationType = ChangesetModificationType.DELETED;
                break;
            default:
                Logging.warn(tr("Unsupported start element ''{0}'' in changeset content at position ({1},{2}). Skipping.",
                        qName, locator.getLineNumber(), locator.getColumnNumber()));
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            switch (qName) {
            case "node":
            case "way":
            case "relation":
                if (currentModificationType == null) {
                    // CHECKSTYLE.OFF: LineLength
                    throwException(tr("Illegal document structure. Found node, way, or relation outside of ''create'', ''modify'', or ''delete''."));
                    // CHECKSTYLE.ON: LineLength
                }
                data.put(currentPrimitive, currentModificationType);
                break;
            case "create":
            case "modify":
            case "delete":
                currentModificationType = null;
                break;
            case "osmChange":
            case "tag":
            case "nd":
            case "member":
                // do nothing
                break;
            default:
                Logging.warn(tr("Unsupported end element ''{0}'' in changeset content at position ({1},{2}). Skipping.",
                        qName, locator.getLineNumber(), locator.getColumnNumber()));
            }
        }

        @Override
        public void error(SAXParseException e) throws SAXException {
            throwException(null, e);
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            throwException(null, e);
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
        this.source = new InputSource(new InputStreamReader(source, StandardCharsets.UTF_8));
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
     * @throws XmlParsingException if something went wrong. Check for chained
     * exceptions.
     */
    public ChangesetDataSet parse(ProgressMonitor progressMonitor) throws XmlParsingException {
        return parse(progressMonitor, false);
    }

    /**
     * Parses the content.
     *
     * @param progressMonitor the progress monitor. Set to {@link NullProgressMonitor#INSTANCE} if null
     * @param useAnonymousUser if true, replace all user information with the anonymous user
     * @return the parsed data
     * @throws XmlParsingException if something went wrong. Check for chained
     * exceptions.
     * @since 14946
     */
    public ChangesetDataSet parse(ProgressMonitor progressMonitor, boolean useAnonymousUser) throws XmlParsingException {
        if (progressMonitor == null) {
            progressMonitor = NullProgressMonitor.INSTANCE;
        }
        try {
            progressMonitor.beginTask("");
            progressMonitor.indeterminateSubTask(tr("Parsing changeset content ..."));
            XmlUtils.parseSafeSAX(source, new Parser(useAnonymousUser));
        } catch (XmlParsingException e) {
            throw e;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new XmlParsingException(e);
        } finally {
            progressMonitor.finishTask();
        }
        return data;
    }

    /**
     * Parses the content from the input source
     *
     * @return the parsed data
     * @throws XmlParsingException if something went wrong. Check for chained
     * exceptions.
     */
    public ChangesetDataSet parse() throws XmlParsingException {
        return parse(null, false);
    }
}
