// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.imagery;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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
            try (InputStream in = new MirroredInputStream(source)) {
                InputSource is = new InputSource(UTFInputStreamReader.create(in));
                factory.newSAXParser().parse(is, parser);
                return parser.entries;
            }
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
            states = new Stack<>();
            states.push(State.INIT);
            entries = new ArrayList<>();
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
                if ("imagery".equals(qName)) {
                    newState = State.IMAGERY;
                }
                break;
            case IMAGERY:
                if ("entry".equals(qName)) {
                    entry = new ImageryInfo();
                    skipEntry = false;
                    newState = State.ENTRY;
                }
                break;
            case ENTRY:
                if (Arrays.asList(new String[] {
                        "name",
                        "id",
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
                } else if ("bounds".equals(qName)) {
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
                } else if ("projections".equals(qName)) {
                    projections = new ArrayList<>();
                    newState = State.PROJECTIONS;
                }
                break;
            case BOUNDS:
                if ("shape".equals(qName)) {
                    shape = new Shape();
                    newState = State.SHAPE;
                }
                break;
            case SHAPE:
                if ("point".equals(qName)) {
                    try {
                        shape.addPoint(atts.getValue("lat"), atts.getValue("lon"));
                    } catch (IllegalArgumentException e) {
                        break;
                    }
                }
                break;
            case PROJECTIONS:
                if ("code".equals(qName)) {
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
            if (newState == State.UNKNOWN && "true".equals(atts.getValue("mandatory"))) {
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
                if ("entry".equals(qName)) {
                    if (!skipEntry) {
                        entries.add(entry);
                    }
                    entry = null;
                }
                break;
            case ENTRY_ATTRIBUTE:
                switch(qName) {
                case "name":
                    entry.setTranslatedName(accumulator.toString());
                    break;
                case "id":
                    entry.setId(accumulator.toString());
                    break;
                case "type":
                    boolean found = false;
                    for (ImageryType type : ImageryType.values()) {
                        if (Objects.equals(accumulator.toString(), type.getTypeString())) {
                            entry.setImageryType(type);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        skipEntry = true;
                    }
                    break;
                case "default":
                    switch (accumulator.toString()) {
                    case "true":
                        entry.setDefaultEntry(true);
                        break;
                    case "false":
                        entry.setDefaultEntry(false);
                        break;
                    default:
                        skipEntry = true;
                    }
                    break;
                case "url":
                    entry.setUrl(accumulator.toString());
                    break;
                case "eula":
                    entry.setEulaAcceptanceRequired(accumulator.toString());
                    break;
                case "min-zoom":
                case "max-zoom":
                    Integer val = null;
                    try {
                        val = Integer.parseInt(accumulator.toString());
                    } catch(NumberFormatException e) {
                        val = null;
                    }
                    if (val == null) {
                        skipEntry = true;
                    } else {
                        if ("min-zoom".equals(qName)) {
                            entry.setDefaultMinZoom(val);
                        } else {
                            entry.setDefaultMaxZoom(val);
                        }
                    }
                    break;
                case "attribution-text":
                    entry.setAttributionText(accumulator.toString());
                    break;
                case "attribution-url":
                    entry.setAttributionLinkURL(accumulator.toString());
                    break;
                case "logo-image":
                    entry.setAttributionImage(accumulator.toString());
                    break;
                case "logo-url":
                    entry.setAttributionImageURL(accumulator.toString());
                    break;
                case "terms-of-use-text":
                    entry.setTermsOfUseText(accumulator.toString());
                    break;
                case "terms-of-use-url":
                    entry.setTermsOfUseURL(accumulator.toString());
                    break;
                case "country-code":
                    entry.setCountryCode(accumulator.toString());
                    break;
                case "icon":
                    entry.setIcon(accumulator.toString());
                    break;
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
