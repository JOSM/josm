// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.Utils.equal;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryBounds;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.data.imagery.Shape;
import org.openstreetmap.josm.io.MirroredInputStream;
import org.openstreetmap.josm.io.UTFInputStreamReader;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ImageryReader {

    private String source;

    private enum State {
        INIT,               // initial state, should always be at the bottom of the stack
        IMAGERY,            // inside the imagery element
        ENTRY,              // inside an entry
        ENTRY_ATTRIBUTE,    // note we are inside an entry attribute to collect the character data
        PROJECTIONS,
        CODE,
        BOUNDS,
        SHAPE,
        UNKNOWN,            // element is not recognized in the current context
    }

    public ImageryReader(String source) {
        this.source = source;
    }

    public List<ImageryInfo> parse() throws SAXException, IOException {
        Parser parser = new Parser();
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            InputStream in = new MirroredInputStream(source);
            InputSource is = new InputSource(UTFInputStreamReader.create(in));
            factory.newSAXParser().parse(is, parser);
            return parser.entries;
        } catch (SAXException e) {
            throw e;
        } catch (ParserConfigurationException e) {
            Main.error(e); // broken SAXException chaining
            throw new SAXException(e);
        }
    }

    private static class Parser extends DefaultHandler {
        private StringBuffer accumulator = new StringBuffer();

        private Stack<State> states;

        List<ImageryInfo> entries;

        /**
         * Skip the current entry because it has mandatory attributes
         * that this version of JOSM cannot process.
         */
        boolean skipEntry;

        ImageryInfo entry;
        ImageryBounds bounds;
        Shape shape;
        List<String> projections;

        @Override public void startDocument() {
            accumulator = new StringBuffer();
            skipEntry = false;
            states = new Stack<State>();
            states.push(State.INIT);
            entries = new ArrayList<ImageryInfo>();
            entry = null;
            bounds = null;
            projections = null;
        }

        @Override
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            accumulator.setLength(0);
            State newState = null;
            switch (states.peek()) {
            case INIT:
                if (qName.equals("imagery")) {
                    newState = State.IMAGERY;
                }
                break;
            case IMAGERY:
                if (qName.equals("entry")) {
                    entry = new ImageryInfo();
                    skipEntry = false;
                    newState = State.ENTRY;
                }
                break;
            case ENTRY:
                if (Arrays.asList(new String[] {
                        "name",
                        "type",
                        "default",
                        "url",
                        "eula",
                        "min-zoom",
                        "max-zoom",
                        "attribution-text",
                        "attribution-url",
                        "logo-image",
                        "logo-url",
                        "terms-of-use-text",
                        "terms-of-use-url",
                        "country-code",
                        "icon",
                }).contains(qName)) {
                    newState = State.ENTRY_ATTRIBUTE;
                } else if (qName.equals("bounds")) {
                    try {
                        bounds = new ImageryBounds(
                                atts.getValue("min-lat") + "," +
                                        atts.getValue("min-lon") + "," +
                                        atts.getValue("max-lat") + "," +
                                        atts.getValue("max-lon"), ",");
                    } catch (IllegalArgumentException e) {
                        break;
                    }
                    newState = State.BOUNDS;
                } else if (qName.equals("projections")) {
                    projections = new ArrayList<String>();
                    newState = State.PROJECTIONS;
                }
                break;
            case BOUNDS:
                if (qName.equals("shape")) {
                    shape = new Shape();
                    newState = State.SHAPE;
                }
                break;
            case SHAPE:
                if (qName.equals("point")) {
                    try {
                        shape.addPoint(atts.getValue("lat"), atts.getValue("lon"));
                    } catch (IllegalArgumentException e) {
                        break;
                    }
                }
                break;
            case PROJECTIONS:
                if (qName.equals("code")) {
                    newState = State.CODE;
                }
                break;
            }
            /**
             * Did not recognize the element, so the new state is UNKNOWN.
             * This includes the case where we are already inside an unknown
             * element, i.e. we do not try to understand the inner content
             * of an unknown element, but wait till it's over.
             */
            if (newState == null) {
                newState = State.UNKNOWN;
            }
            states.push(newState);
            if (newState == State.UNKNOWN && equal(atts.getValue("mandatory"), "true")) {
                skipEntry = true;
            }
            return;
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            accumulator.append(ch, start, length);
        }

        @Override
        public void endElement(String namespaceURI, String qName, String rqName) {
            switch (states.pop()) {
            case INIT:
                throw new RuntimeException("parsing error: more closing than opening elements");
            case ENTRY:
                if (qName.equals("entry")) {
                    if (!skipEntry) {
                        entries.add(entry);
                    }
                    entry = null;
                }
                break;
            case ENTRY_ATTRIBUTE:
                if (qName.equals("name")) {
                    entry.setName(tr(accumulator.toString()));
                } else if (qName.equals("type")) {
                    boolean found = false;
                    for (ImageryType type : ImageryType.values()) {
                        if (equal(accumulator.toString(), type.getTypeString())) {
                            entry.setImageryType(type);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        skipEntry = true;
                    }
                } else if (qName.equals("default")) {
                    if (accumulator.toString().equals("true")) {
                        entry.setDefaultEntry(true);
                    } else if (accumulator.toString().equals("false")) {
                        entry.setDefaultEntry(false);
                    } else {
                        skipEntry = true;
                    }
                } else if (qName.equals("url")) {
                    entry.setUrl(accumulator.toString());
                } else if (qName.equals("eula")) {
                    entry.setEulaAcceptanceRequired(accumulator.toString());
                } else if (qName.equals("min-zoom") || qName.equals("max-zoom")) {
                    Integer val = null;
                    try {
                        val = Integer.parseInt(accumulator.toString());
                    } catch(NumberFormatException e) {
                        val = null;
                    }
                    if (val == null) {
                        skipEntry = true;
                    } else {
                        if (qName.equals("min-zoom")) {
                            entry.setDefaultMinZoom(val);
                        } else {
                            entry.setDefaultMaxZoom(val);
                        }
                    }
                } else if (qName.equals("attribution-text")) {
                    entry.setAttributionText(accumulator.toString());
                } else if (qName.equals("attribution-url")) {
                    entry.setAttributionLinkURL(accumulator.toString());
                } else if (qName.equals("logo-image")) {
                    entry.setAttributionImage(accumulator.toString());
                } else if (qName.equals("logo-url")) {
                    entry.setAttributionImageURL(accumulator.toString());
                } else if (qName.equals("terms-of-use-text")) {
                    entry.setTermsOfUseText(accumulator.toString());
                } else if (qName.equals("terms-of-use-url")) {
                    entry.setTermsOfUseURL(accumulator.toString());
                } else if (qName.equals("country-code")) {
                    entry.setCountryCode(accumulator.toString());
                } else if (qName.equals("icon")) {
                    entry.setIcon(accumulator.toString());
                }
                break;
            case BOUNDS:
                entry.setBounds(bounds);
                bounds = null;
                break;
            case SHAPE:
                bounds.addShape(shape);
                shape = null;
                break;
            case CODE:
                projections.add(accumulator.toString());
                break;
            case PROJECTIONS:
                entry.setServerProjections(projections);
                projections = null;
                break;
            }
        }
    }
}
