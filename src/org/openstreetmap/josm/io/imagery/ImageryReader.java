// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.imagery;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.ParserConfigurationException;

import org.openstreetmap.josm.data.imagery.DefaultLayer;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryBounds;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.data.imagery.Shape;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.LanguageInfo;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.MultiMap;
import org.openstreetmap.josm.tools.Utils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Reader to parse the list of available imagery servers from an XML definition file.
 * <p>
 * The format is specified in the <a href="https://josm.openstreetmap.de/wiki/Maps">JOSM wiki</a>.
 */
public class ImageryReader implements Closeable {

    private final String source;
    private CachedFile cachedFile;
    private boolean fastFail;

    private enum State {
        INIT,               // initial state, should always be at the bottom of the stack
        IMAGERY,            // inside the imagery element
        ENTRY,              // inside an entry
        ENTRY_ATTRIBUTE,    // note we are inside an entry attribute to collect the character data
        PROJECTIONS,        // inside projections block of an entry
        MIRROR,             // inside an mirror entry
        MIRROR_ATTRIBUTE,   // note we are inside an mirror attribute to collect the character data
        MIRROR_PROJECTIONS, // inside projections block of an mirror entry
        CODE,
        BOUNDS,
        SHAPE,
        NO_TILE,
        NO_TILESUM,
        METADATA,
        DEFAULT_LAYERS,
        CUSTOM_HTTP_HEADERS,
        NOOP,
        UNKNOWN,             // element is not recognized in the current context
    }

    /**
     * Constructs a {@code ImageryReader} from a given filename, URL or internal resource.
     *
     * @param source can be:<ul>
     *  <li>relative or absolute file name</li>
     *  <li>{@code file:///SOME/FILE} the same as above</li>
     *  <li>{@code http://...} a URL. It will be cached on disk.</li>
     *  <li>{@code resource://SOME/FILE} file from the classpath (usually in the current *.jar)</li>
     *  <li>{@code josmdir://SOME/FILE} file inside josm user data directory (since r7058)</li>
     *  <li>{@code josmplugindir://SOME/FILE} file inside josm plugin directory (since r7834)</li></ul>
     */
    public ImageryReader(String source) {
        this.source = source;
    }

    /**
     * Parses imagery source.
     * @return list of imagery info
     * @throws SAXException if any SAX error occurs
     * @throws IOException if any I/O error occurs
     */
    public List<ImageryInfo> parse() throws SAXException, IOException {
        Parser parser = new Parser();
        try {
            cachedFile = new CachedFile(source);
            cachedFile.setParam(Utils.join(",", ImageryInfo.getActiveIds()));
            cachedFile.setFastFail(fastFail);
            try (BufferedReader in = cachedFile
                    .setMaxAge(CachedFile.DAYS)
                    .setCachingStrategy(CachedFile.CachingStrategy.IfModifiedSince)
                    .getContentReader()) {
                InputSource is = new InputSource(in);
                Utils.parseSafeSAX(is, parser);
                return parser.entries;
            }
        } catch (SAXException e) {
            throw e;
        } catch (ParserConfigurationException e) {
            Logging.error(e); // broken SAXException chaining
            throw new SAXException(e);
        }
    }

    private static class Parser extends DefaultHandler {
        private static final String MAX_ZOOM = "max-zoom";
        private static final String MIN_ZOOM = "min-zoom";
        private static final String TILE_SIZE = "tile-size";
        private static final String TRUE = "true";

        private StringBuilder accumulator = new StringBuilder();

        private Stack<State> states;

        private List<ImageryInfo> entries;

        /**
         * Skip the current entry because it has mandatory attributes
         * that this version of JOSM cannot process.
         */
        private boolean skipEntry;

        private ImageryInfo entry;
        /** In case of mirror parsing this contains the mirror entry */
        private ImageryInfo mirrorEntry;
        private ImageryBounds bounds;
        private Shape shape;
        // language of last element, does only work for simple ENTRY_ATTRIBUTE's
        private String lang;
        private List<String> projections;
        private MultiMap<String, String> noTileHeaders;
        private MultiMap<String, String> noTileChecksums;
        private Map<String, String> metadataHeaders;
        private List<DefaultLayer> defaultLayers;
        private Map<String, String> customHttpHeaders;

        @Override
        public void startDocument() {
            accumulator = new StringBuilder();
            skipEntry = false;
            states = new Stack<>();
            states.push(State.INIT);
            entries = new ArrayList<>();
            entry = null;
            bounds = null;
            projections = null;
            noTileHeaders = null;
            noTileChecksums = null;
            customHttpHeaders = null;
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
                    noTileHeaders = new MultiMap<>();
                    noTileChecksums = new MultiMap<>();
                    metadataHeaders = new ConcurrentHashMap<>();
                    defaultLayers = new ArrayList<>();
                    customHttpHeaders = new ConcurrentHashMap<>();
                    String best = atts.getValue("eli-best");
                    if (TRUE.equals(best)) {
                        entry.setBestMarked(true);
                    }
                    String overlay = atts.getValue("overlay");
                    if (TRUE.equals(overlay)) {
                        entry.setOverlay(true);
                    }
                }
                break;
            case MIRROR:
                if (Arrays.asList(
                        "type",
                        "url",
                        "id",
                        MIN_ZOOM,
                        MAX_ZOOM,
                        TILE_SIZE
                ).contains(qName)) {
                    newState = State.MIRROR_ATTRIBUTE;
                    lang = atts.getValue("lang");
                } else if ("projections".equals(qName)) {
                    projections = new ArrayList<>();
                    newState = State.MIRROR_PROJECTIONS;
                }
                break;
            case ENTRY:
                if (Arrays.asList(
                        "name",
                        "id",
                        "oldid",
                        "type",
                        "description",
                        "default",
                        "url",
                        "eula",
                        MIN_ZOOM,
                        MAX_ZOOM,
                        "attribution-text",
                        "attribution-url",
                        "logo-image",
                        "logo-url",
                        "terms-of-use-text",
                        "terms-of-use-url",
                        "permission-ref",
                        "country-code",
                        "icon",
                        "date",
                        TILE_SIZE,
                        "valid-georeference",
                        "mod-tile-features",
                        "transparent",
                        "minimum-tile-expire"
                ).contains(qName)) {
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
                        Logging.trace(e);
                        break;
                    }
                    newState = State.BOUNDS;
                } else if ("projections".equals(qName)) {
                    projections = new ArrayList<>();
                    newState = State.PROJECTIONS;
                } else if ("mirror".equals(qName)) {
                    projections = new ArrayList<>();
                    newState = State.MIRROR;
                    mirrorEntry = new ImageryInfo();
                } else if ("no-tile-header".equals(qName)) {
                    noTileHeaders.put(atts.getValue("name"), atts.getValue("value"));
                    newState = State.NO_TILE;
                } else if ("no-tile-checksum".equals(qName)) {
                    noTileChecksums.put(atts.getValue("type"), atts.getValue("value"));
                    newState = State.NO_TILESUM;
                } else if ("metadata-header".equals(qName)) {
                    metadataHeaders.put(atts.getValue("header-name"), atts.getValue("metadata-key"));
                    newState = State.METADATA;
                } else if ("defaultLayers".equals(qName)) {
                    newState = State.DEFAULT_LAYERS;
                } else if ("custom-http-header".equals(qName)) {
                   customHttpHeaders.put(atts.getValue("header-name"), atts.getValue("header-value"));
                   newState = State.CUSTOM_HTTP_HEADERS;
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
                        Logging.trace(e);
                        break;
                    }
                }
                break;
            case PROJECTIONS:
            case MIRROR_PROJECTIONS:
                if ("code".equals(qName)) {
                    newState = State.CODE;
                }
                break;
            case DEFAULT_LAYERS:
                if ("layer".equals(qName)) {
                    newState = State.NOOP;
                    defaultLayers.add(new DefaultLayer(
                            entry.getImageryType(),
                            atts.getValue("name"),
                            atts.getValue("style"),
                            atts.getValue("tileMatrixSet")
                            ));
                }
                break;
            default: // Do nothing
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
            if (newState == State.UNKNOWN && TRUE.equals(atts.getValue("mandatory"))) {
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
                throw new JosmRuntimeException("parsing error: more closing than opening elements");
            case ENTRY:
                if ("entry".equals(qName)) {
                    entry.setNoTileHeaders(noTileHeaders);
                    noTileHeaders = null;
                    entry.setNoTileChecksums(noTileChecksums);
                    noTileChecksums = null;
                    entry.setMetadataHeaders(metadataHeaders);
                    metadataHeaders = null;
                    entry.setDefaultLayers(defaultLayers);
                    defaultLayers = null;
                    entry.setCustomHttpHeaders(customHttpHeaders);
                    customHttpHeaders = null;

                    if (!skipEntry) {
                        entries.add(entry);
                    }
                    entry = null;
                }
                break;
            case MIRROR:
                if (mirrorEntry != null && "mirror".equals(qName)) {
                    entry.addMirror(mirrorEntry);
                    mirrorEntry = null;
                }
                break;
            case MIRROR_ATTRIBUTE:
                if (mirrorEntry != null) {
                    switch(qName) {
                    case "type":
                        ImageryType.values();
                        boolean found = false;
                        for (ImageryType type : ImageryType.values()) {
                            if (Objects.equals(accumulator.toString(), type.getTypeString())) {
                                mirrorEntry.setImageryType(type);
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            mirrorEntry = null;
                        }
                        break;
                    case "id":
                        mirrorEntry.setId(accumulator.toString());
                        break;
                    case "url":
                        mirrorEntry.setUrl(accumulator.toString());
                        break;
                    case MIN_ZOOM:
                    case MAX_ZOOM:
                        Integer val = null;
                        try {
                            val = Integer.valueOf(accumulator.toString());
                        } catch (NumberFormatException e) {
                            val = null;
                        }
                        if (val == null) {
                            mirrorEntry = null;
                        } else {
                            if (MIN_ZOOM.equals(qName)) {
                                mirrorEntry.setDefaultMinZoom(val);
                            } else {
                                mirrorEntry.setDefaultMaxZoom(val);
                            }
                        }
                        break;
                    case TILE_SIZE:
                        Integer tileSize = null;
                        try {
                            tileSize = Integer.valueOf(accumulator.toString());
                        } catch (NumberFormatException e) {
                            tileSize = null;
                        }
                        if (tileSize == null) {
                            mirrorEntry = null;
                        } else {
                            entry.setTileSize(tileSize.intValue());
                        }
                        break;
                    default: // Do nothing
                    }
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
                case "date":
                    entry.setDate(accumulator.toString());
                    break;
                case "id":
                    entry.setId(accumulator.toString());
                    break;
                case "oldid":
                    entry.addOldId(accumulator.toString());
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
                    case TRUE:
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
                case MIN_ZOOM:
                case MAX_ZOOM:
                    Integer val = null;
                    try {
                        val = Integer.valueOf(accumulator.toString());
                    } catch (NumberFormatException e) {
                        val = null;
                    }
                    if (val == null) {
                        skipEntry = true;
                    } else {
                        if (MIN_ZOOM.equals(qName)) {
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
                case "permission-ref":
                    entry.setPermissionReferenceURL(accumulator.toString());
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
                case TILE_SIZE:
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
                case "valid-georeference":
                    entry.setGeoreferenceValid(Boolean.parseBoolean(accumulator.toString()));
                    break;
                case "mod-tile-features":
                    entry.setModTileFeatures(Boolean.parseBoolean(accumulator.toString()));
                    break;
                case "transparent":
                    entry.setTransparent(Boolean.parseBoolean(accumulator.toString()));
                    break;
                case "minimum-tile-expire":
                    entry.setMinimumTileExpire(Integer.valueOf(accumulator.toString()));
                    break;
                default: // Do nothing
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
            case MIRROR_PROJECTIONS:
                mirrorEntry.setServerProjections(projections);
                projections = null;
                break;
            case NO_TILE:
            case NO_TILESUM:
            case METADATA:
            case UNKNOWN:
            default:
                // nothing to do for these or the unknown type
            }
        }
    }

    /**
     * Sets whether opening HTTP connections should fail fast, i.e., whether a
     * {@link HttpClient#setConnectTimeout(int) low connect timeout} should be used.
     * @param fastFail whether opening HTTP connections should fail fast
     * @see CachedFile#setFastFail(boolean)
     */
    public void setFastFail(boolean fastFail) {
        this.fastFail = fastFail;
    }

    @Override
    public void close() throws IOException {
        Utils.close(cachedFile);
    }
}
