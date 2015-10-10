// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.imagery;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.xml.parsers.ParserConfigurationException;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryBounds;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.data.imagery.Shape;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.io.UTFInputStreamReader;
import org.openstreetmap.josm.tools.LanguageInfo;
import org.openstreetmap.josm.tools.Utils;
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
        NO_TILE,
        METADATA,
        UNKNOWN,            // element is not recognized in the current context
    }

    public ImageryReader(String source) {
        this.source = source;
    }

    public List<ImageryInfo> parse() throws SAXException, IOException {
        Parser parser = new Parser();
        try {
            try (InputStream in = new CachedFile(source)
                    .setMaxAge(1*CachedFile.DAYS)
                    .setCachingStrategy(CachedFile.CachingStrategy.IfModifiedSince)
                    .getInputStream()) {
                InputSource is = new InputSource(UTFInputStreamReader.create(in));
                Utils.parseSafeSAX(is, parser);
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
        private StringBuilder accumulator = new StringBuilder();

        private Deque<State> states;

        private List<ImageryInfo> entries;

        /**
         * Skip the current entry because it has mandatory attributes
         * that this version of JOSM cannot process.
         */
        private boolean skipEntry;

        private ImageryInfo entry;
        private ImageryBounds bounds;
        private Shape shape;
        // language of last element, does only work for simple ENTRY_ATTRIBUTE's
        private String lang;
        private List<String> projections;
        private Map<String, String> noTileHeaders;
        private Map<String, String> metadataHeaders;

        @Override
        public void startDocument() {
            accumulator = new StringBuilder();
            skipEntry = false;
            states = new ArrayDeque<>();
            states.push(State.INIT);
            entries = new ArrayList<>();
            entry = null;
            bounds = null;
            projections = null;
            noTileHeaders = null;
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
                    noTileHeaders = new HashMap<>();
                    metadataHeaders = new HashMap<>();
                }
                break;
            case ENTRY:
                if (Arrays.asList(new String[] {
                        "name",
                        "id",
                        "type",
                        "description",
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
                        "tile-size",
                }).contains(qName)) {
                    newState = State.ENTRY_ATTRIBUTE;
                    lang = atts.getValue("lang");
                } else if ("bounds".equals(qName)) {
                    try {
                        bounds = new ImageryBounds(
                                atts.getValue("min-lat") + ',' +
                                        atts.getValue("min-lon") + ',' +
                                        atts.getValue("max-lat") + ',' +
                                        atts.getValue("max-lon"), ",");
                    } catch (IllegalArgumentException e) {
                        break;
                    }
                    newState = State.BOUNDS;
                } else if ("projections".equals(qName)) {
                    projections = new ArrayList<>();
                    newState = State.PROJECTIONS;
                } else if ("no-tile-header".equals(qName)) {
                    noTileHeaders.put(atts.getValue("name"), atts.getValue("value"));
                    newState = State.NO_TILE;
                } else if ("metadata-header".equals(qName)) {
                    metadataHeaders.put(atts.getValue("header-name"), atts.getValue("metadata-key"));
                    newState = State.METADATA;
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
                    entry.setNoTileHeaders(noTileHeaders);
                    noTileHeaders = null;
                    entry.setMetadataHeaders(metadataHeaders);
                    metadataHeaders = null;

                    if (!skipEntry) {
                        entries.add(entry);
                    }
                    entry = null;
                }
                break;
            case ENTRY_ATTRIBUTE:
                switch(qName) {
                case "name":
                    entry.setName(lang == null ? LanguageInfo.getJOSMLocaleCode(null) : lang, accumulator.toString());
                    break;
                case "description":
                    entry.setDescription(lang, accumulator.toString());
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
                        val = Integer.valueOf(accumulator.toString());
                    } catch (NumberFormatException e) {
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
                case "tile-size":
                    Integer tileSize = null;
                    try {
                        tileSize = Integer.valueOf(accumulator.toString());
                    } catch (NumberFormatException e) {
                        tileSize = null;
                    }
                    if (tileSize == null) {
                        skipEntry = true;
                    } else {
                        entry.setTileSize(tileSize.intValue());
                    }
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
            case NO_TILE:
                break;

            }
        }
    }
}
