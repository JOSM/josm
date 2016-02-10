// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.io.CachedFile;
import org.xml.sax.SAXException;

/**
 * Loads preferences from XML.
 */
public class PreferencesReader {

    private final SortedMap<String, Setting<?>> settings = new TreeMap<>();
    private int version = 0;
    private XMLStreamReader parser;

    /**
     * Validate the XML.
     * @param f the file
     * @throws IOException if any I/O error occurs
     * @throws SAXException if any SAX error occurs
     */
    public static void validateXML(File f) throws IOException, SAXException {
        try (BufferedReader in = Files.newBufferedReader(f.toPath(), StandardCharsets.UTF_8)) {
            validateXML(in);
        }
    }

    /**
     * Validate the XML.
     * @param in the {@link Reader}
     * @throws IOException if any I/O error occurs
     * @throws SAXException if any SAX error occurs
     */
    public static void validateXML(Reader in) throws IOException, SAXException {
        try (InputStream xsdStream = new CachedFile("resource://data/preferences.xsd").getInputStream()) {
            Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(new StreamSource(xsdStream));
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(in));
        }
    }

    /**
     * Parse preferences XML.
     * @param f the file
     * @throws IOException if any I/O error occurs
     * @throws XMLStreamException if any XML stream error occurs
     */
    public void fromXML(File f) throws IOException, XMLStreamException {
        try (BufferedReader in = Files.newBufferedReader(f.toPath(), StandardCharsets.UTF_8)) {
            fromXML(in);
        }
    }

    /**
     * Parse preferences XML.
     * @param in the {@link Reader}
     * @throws XMLStreamException if any XML stream error occurs
     */
    public void fromXML(Reader in) throws XMLStreamException {
        this.parser = XMLInputFactory.newInstance().createXMLStreamReader(in);
        parse();
    }

    /**
     * Return the parsed preferences as a settings map
     * @return the parsed preferences as a settings map
     */
    public SortedMap<String, Setting<?>> getSettings() {
        return settings;
    }

    /**
     * Return the version from the XML root element.
     * (Represents the JOSM version when the file was written.)
     * @return the version
     */
    public int getVersion() {
        return version;
    }

    private void parse() throws XMLStreamException {
        int event = parser.getEventType();
        while (true) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                try {
                    version = Integer.parseInt(parser.getAttributeValue(null, "version"));
                } catch (NumberFormatException e) {
                    if (Main.isDebugEnabled()) {
                        Main.debug(e.getMessage());
                    }
                }
                parseRoot();
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                return;
            }
            if (parser.hasNext()) {
                event = parser.next();
            } else {
                break;
            }
        }
        parser.close();
    }

    private void parseRoot() throws XMLStreamException {
        while (true) {
            int event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String localName = parser.getLocalName();
                switch(localName) {
                case "tag":
                    settings.put(parser.getAttributeValue(null, "key"), new StringSetting(parser.getAttributeValue(null, "value")));
                    jumpToEnd();
                    break;
                case "list":
                case "collection":
                case "lists":
                case "maps":
                    parseToplevelList();
                    break;
                default:
                    throwException("Unexpected element: "+localName);
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                return;
            }
        }
    }

    private void jumpToEnd() throws XMLStreamException {
        while (true) {
            int event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                jumpToEnd();
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                return;
            }
        }
    }

    private void parseToplevelList() throws XMLStreamException {
        String key = parser.getAttributeValue(null, "key");
        String name = parser.getLocalName();

        List<String> entries = null;
        List<List<String>> lists = null;
        List<Map<String, String>> maps = null;
        while (true) {
            int event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String localName = parser.getLocalName();
                switch(localName) {
                case "entry":
                    if (entries == null) {
                        entries = new ArrayList<>();
                    }
                    entries.add(parser.getAttributeValue(null, "value"));
                    jumpToEnd();
                    break;
                case "list":
                    if (lists == null) {
                        lists = new ArrayList<>();
                    }
                    lists.add(parseInnerList());
                    break;
                case "map":
                    if (maps == null) {
                        maps = new ArrayList<>();
                    }
                    maps.add(parseMap());
                    break;
                default:
                    throwException("Unexpected element: "+localName);
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                break;
            }
        }
        if (entries != null) {
            settings.put(key, new ListSetting(Collections.unmodifiableList(entries)));
        } else if (lists != null) {
            settings.put(key, new ListListSetting(Collections.unmodifiableList(lists)));
        } else if (maps != null) {
            settings.put(key, new MapListSetting(Collections.unmodifiableList(maps)));
        } else {
            if ("lists".equals(name)) {
                settings.put(key, new ListListSetting(Collections.<List<String>>emptyList()));
            } else if ("maps".equals(name)) {
                settings.put(key, new MapListSetting(Collections.<Map<String, String>>emptyList()));
            } else {
                settings.put(key, new ListSetting(Collections.<String>emptyList()));
            }
        }
    }

    private List<String> parseInnerList() throws XMLStreamException {
        List<String> entries = new ArrayList<>();
        while (true) {
            int event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("entry".equals(parser.getLocalName())) {
                    entries.add(parser.getAttributeValue(null, "value"));
                    jumpToEnd();
                } else {
                    throwException("Unexpected element: "+parser.getLocalName());
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                break;
            }
        }
        return Collections.unmodifiableList(entries);
    }

    private Map<String, String> parseMap() throws XMLStreamException {
        Map<String, String> map = new LinkedHashMap<>();
        while (true) {
            int event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("tag".equals(parser.getLocalName())) {
                    map.put(parser.getAttributeValue(null, "key"), parser.getAttributeValue(null, "value"));
                    jumpToEnd();
                } else {
                    throwException("Unexpected element: "+parser.getLocalName());
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                break;
            }
        }
        return Collections.unmodifiableMap(map);
    }

    private void throwException(String msg) {
        throw new RuntimeException(msg + tr(" (at line {0}, column {1})",
                parser.getLocation().getLineNumber(), parser.getLocation().getColumnNumber()));
    }
}
