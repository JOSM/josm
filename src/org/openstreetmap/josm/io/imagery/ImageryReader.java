// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.Utils.equal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.io.MirroredInputStream;
import org.openstreetmap.josm.io.UTFInputStreamReader;
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
        SUPPORTED_PROJECTIONS,
        PR,
        UNKNOWN,            // element is not recognized in the current context
    }

    public ImageryReader(String source) throws IOException {
        this.source = source;
    }

    public List<ImageryInfo> parse() throws SAXException, IOException {
        if (isXml(source)) {
            Parser parser = new Parser();
            try {
                SAXParserFactory factory = SAXParserFactory.newInstance();
                factory.setNamespaceAware(true);
                InputStream in = new MirroredInputStream(source);
                InputSource is = new InputSource(UTFInputStreamReader.create(in, "UTF-8"));
                factory.newSAXParser().parse(is, parser);
                return parser.entries;
            } catch (SAXException e) {
                throw e;
            } catch (ParserConfigurationException e) {
                e.printStackTrace(); // broken SAXException chaining
                throw new SAXException(e);
            }
        } else {
            return readCSV(source);
        }
    }

    /**
     * Probe the file to see if it is xml or the traditional csv format.
     * 
     * If the first non-whitespace character is a '<', decide for
     * xml, otherwise csv.
     */
    private boolean isXml(String source) {
        MirroredInputStream in = null;
        try {
            in = new MirroredInputStream(source);
            InputStreamReader reader = UTFInputStreamReader.create(in, null);
            WHILE: while (true) {
                int c = reader.read();
                switch (c) {
                    case -1:
                        break WHILE;
                    case ' ':
                    case '\t':
                    case '\n':
                    case '\r':
                        continue;
                    case '<':
                        return true;
                    default:
                        return false;
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            Utils.close(in);
        }
        Main.warn(tr("Warning: Could not detect type of imagery source '{0}'. Using default (xml).", source));
        return true;
    }

    private List<ImageryInfo> readCSV(String source) {
        List<ImageryInfo> entries = new ArrayList<ImageryInfo>();
        MirroredInputStream s = null;
        try {
            s = new MirroredInputStream(source);
            try {
                InputStreamReader r;
                try
                {
                    r = new InputStreamReader(s, "UTF-8");
                }
                catch (UnsupportedEncodingException e)
                {
                    r = new InputStreamReader(s);
                }
                BufferedReader reader = new BufferedReader(r);
                String line;
                while((line = reader.readLine()) != null)
                {
                    String val[] = line.split(";");
                    if(!line.startsWith("#") && val.length >= 3) {
                        boolean defaultEntry = "true".equals(val[0]);
                        String name = tr(val[1]);
                        String url = val[2];
                        String eulaAcceptanceRequired = null;

                        if (val.length >= 4 && !val[3].isEmpty()) {
                            // 4th parameter optional for license agreement (EULA)
                            eulaAcceptanceRequired = val[3];
                        }

                        ImageryInfo info = new ImageryInfo(name, url, eulaAcceptanceRequired);
                        
                        info.setDefaultEntry(defaultEntry);

                        if (val.length >= 5 && !val[4].isEmpty()) {
                            // 5th parameter optional for bounds
                            try {
                                info.setBounds(new Bounds(val[4], ","));
                            } catch (IllegalArgumentException e) {
                                Main.warn(e.toString());
                            }
                        }
                        if (val.length >= 6 && !val[5].isEmpty()) {
                            info.setAttributionText(val[5]);
                        }
                        if (val.length >= 7 && !val[6].isEmpty()) {
                            info.setAttributionLinkURL(val[6]);
                        }
                        if (val.length >= 8 && !val[7].isEmpty()) {
                            info.setTermsOfUseURL(val[7]);
                        }
                        if (val.length >= 9 && !val[8].isEmpty()) {
                            info.setAttributionImage(val[8]);
                        }

                        entries.add(info);
                    }
                }
            } finally {
                Utils.close(s);
            }
            return entries;
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            Utils.close(s);
        }
        return entries;
    }

    private class Parser extends DefaultHandler {
        private StringBuffer accumulator = new StringBuffer();

        private Stack<State> states;

        List<ImageryInfo> entries;

        /**
         * Skip the current entry because it has mandatory attributes
         * that this version of JOSM cannot process.
         */
        boolean skipEntry;

        ImageryInfo entry;
        Bounds bounds;
        List<String> supported_srs;

        @Override public void startDocument() {
            accumulator = new StringBuffer();
            skipEntry = false;
            states = new Stack<State>();
            states.push(State.INIT);
            entries = new ArrayList<ImageryInfo>();
            entry = null;
            bounds = null;
            supported_srs = null;
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
                    }).contains(qName)) {
                        newState = State.ENTRY_ATTRIBUTE;
                    } else if (qName.equals("bounds")) {
                        try {
                            bounds = new Bounds(
                                    atts.getValue("min-lat") + "," +
                                    atts.getValue("min-lon") + "," +
                                    atts.getValue("max-lat") + "," +
                                    atts.getValue("max-lon"), ",");
                        } catch (IllegalArgumentException e) {
                            break;
                        }
                        newState = State.ENTRY_ATTRIBUTE;
                    } else if (qName.equals("supported-projections")) {
                        supported_srs = new ArrayList<String>();
                        newState = State.SUPPORTED_PROJECTIONS;
                    }
                    break;
                case SUPPORTED_PROJECTIONS:
                    if (qName.equals("pr")) {
                        newState = State.PR;
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
                        entry.setName(accumulator.toString());
                    } else if (qName.equals("type")) {
                        boolean found = false;
                        for (ImageryType type : ImageryType.values()) {
                            if (equal(accumulator.toString(), type.getUrlString())) {
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
                                entry.setMaxZoom(val);
                            }
                        }
                    } else if (qName.equals("bounds")) {
                        entry.setBounds(bounds);
                        bounds = null;
                    } else if (qName.equals("attribution-text")) {
                        entry.setAttributionText(accumulator.toString());
                    } else if (qName.equals("attribution-url")) {
                        entry.setAttributionLinkURL(accumulator.toString());
                    } else if (qName.equals("logo-image")) {
                        entry.setAttributionImage(accumulator.toString());
                    } else if (qName.equals("logo-url")) {
                        // TODO: it should be possible to specify the link for the logo
                    } else if (qName.equals("terms-of-use-text")) {
                        // TODO: it should be possible to configure the terms of use display text
                    } else if (qName.equals("terms-of-use-url")) {
                        entry.setTermsOfUseURL(accumulator.toString());
                    }
                    break;
                case PR:
                    supported_srs.add(accumulator.toString());
                    break;
                case SUPPORTED_PROJECTIONS:
                    entry.setServerProjections(supported_srs);
                    supported_srs = null;
                    break;
            }
        }
    }
}
