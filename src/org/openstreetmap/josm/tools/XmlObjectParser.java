// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.ValidatorHandler;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.io.MirroredInputStream;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * An helper class that reads from a XML stream into specific objects.
 *
 * @author Imi
 */
public class XmlObjectParser implements Iterable<Object> {
    public static class PresetParsingException extends SAXException {
        private int columnNumber;
        private int lineNumber;

        /**
         * Constructs a new {@code PresetParsingException}.
         */
        public PresetParsingException() {
            super();
        }

        public PresetParsingException(Exception e) {
            super(e);
        }

        public PresetParsingException(String message, Exception e) {
            super(message, e);
        }

        public PresetParsingException(String message) {
            super(message);
        }

        public PresetParsingException rememberLocation(Locator locator) {
            if (locator == null) return this;
            this.columnNumber = locator.getColumnNumber();
            this.lineNumber = locator.getLineNumber();
            return this;
        }

        @Override
        public String getMessage() {
            String msg = super.getMessage();
            if (lineNumber == 0 && columnNumber == 0)
                return msg;
            if (msg == null) {
                msg = getClass().getName();
            }
            msg = msg + " " + tr("(at line {0}, column {1})", lineNumber, columnNumber);
            return msg;
        }

        public int getColumnNumber() {
            return columnNumber;
        }

        public int getLineNumber() {
            return lineNumber;
        }
    }

    public static final String lang = LanguageInfo.getLanguageCodeXML();

    private static class AddNamespaceFilter extends XMLFilterImpl {

        private final String namespace;

        public AddNamespaceFilter(String namespace) {
            this.namespace = namespace;
        }

        @Override
        public void startElement (String uri, String localName, String qName, Attributes atts) throws SAXException {
            if ("".equals(uri)) {
                super.startElement(namespace, localName, qName, atts);
            } else {
                super.startElement(uri, localName, qName, atts);
            }

        }

    }

    private class Parser extends DefaultHandler {
        Stack<Object> current = new Stack<Object>();
        StringBuilder characters = new StringBuilder(64);

        private Locator locator;

        @Override
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        protected void throwException(Exception e) throws PresetParsingException{
            throw new PresetParsingException(e).rememberLocation(locator);
        }

        @Override public void startElement(String ns, String lname, String qname, Attributes a) throws SAXException {
            if (mapping.containsKey(qname)) {
                Class<?> klass = mapping.get(qname).klass;
                try {
                    current.push(klass.newInstance());
                } catch (Exception e) {
                    throwException(e);
                }
                for (int i = 0; i < a.getLength(); ++i) {
                    setValue(mapping.get(qname), a.getQName(i), a.getValue(i));
                }
                if (mapping.get(qname).onStart) {
                    report();
                }
                if (mapping.get(qname).both) {
                    queue.add(current.peek());
                }
            }
        }
        @Override public void endElement(String ns, String lname, String qname) throws SAXException {
            if (mapping.containsKey(qname) && !mapping.get(qname).onStart) {
                report();
            } else if (mapping.containsKey(qname) && characters != null && !current.isEmpty()) {
                setValue(mapping.get(qname), qname, characters.toString().trim());
                characters  = new StringBuilder(64);
            }
        }
        @Override public void characters(char[] ch, int start, int length) {
            characters.append(ch, start, length);
        }

        private void report() {
            queue.add(current.pop());
            characters  = new StringBuilder(64);
        }

        private Object getValueForClass(Class<?> klass, String value) {
            if (klass == Boolean.TYPE)
                return parseBoolean(value);
            else if (klass == Integer.TYPE || klass == Long.TYPE)
                return Long.parseLong(value);
            else if (klass == Float.TYPE || klass == Double.TYPE)
                return Double.parseDouble(value);
            return value;
        }

        private void setValue(Entry entry, String fieldName, String value) throws SAXException {
            CheckParameterUtil.ensureParameterNotNull(entry, "entry");
            if (fieldName.equals("class") || fieldName.equals("default") || fieldName.equals("throw") || fieldName.equals("new") || fieldName.equals("null")) {
                fieldName += "_";
            }
            try {
                Object c = current.peek();
                Field f = entry.getField(fieldName);
                if (f == null && fieldName.startsWith(lang)) {
                    f = entry.getField("locale_" + fieldName.substring(lang.length()));
                }
                if (f != null && Modifier.isPublic(f.getModifiers()) && (
                        String.class.equals(f.getType()) || boolean.class.equals(f.getType()))) {
                    f.set(c, getValueForClass(f.getType(), value));
                } else {
                    if (fieldName.startsWith(lang)) {
                        int l = lang.length();
                        fieldName = "set" + fieldName.substring(l, l + 1).toUpperCase(Locale.ENGLISH) + fieldName.substring(l + 1);
                    } else {
                        fieldName = "set" + fieldName.substring(0, 1).toUpperCase(Locale.ENGLISH) + fieldName.substring(1);
                    }
                    Method m = entry.getMethod(fieldName);
                    if (m != null) {
                        m.invoke(c, new Object[]{getValueForClass(m.getParameterTypes()[0], value)});
                    }
                }
            } catch (Exception e) {
                Main.error(e); // SAXException does not dump inner exceptions.
                throwException(e);
            }
        }

        private boolean parseBoolean(String s) {
            return s != null
                    && !s.equals("0")
                    && !s.startsWith("off")
                    && !s.startsWith("false")
                    && !s.startsWith("no");
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

    private static class Entry {
        Class<?> klass;
        boolean onStart;
        boolean both;
        private final Map<String, Field> fields = new HashMap<String, Field>();
        private final Map<String, Method> methods = new HashMap<String, Method>();

        public Entry(Class<?> klass, boolean onStart, boolean both) {
            this.klass = klass;
            this.onStart = onStart;
            this.both = both;
        }

        Field getField(String s) {
            if (fields.containsKey(s)) {
                return fields.get(s);
            } else {
                try {
                    Field f = klass.getField(s);
                    fields.put(s, f);
                    return f;
                } catch (NoSuchFieldException ex) {
                    fields.put(s, null);
                    return null;
                }
            }
        }

        Method getMethod(String s) {
            if (methods.containsKey(s)) {
                return methods.get(s);
            } else {
                for (Method m : klass.getMethods()) {
                    if (m.getName().equals(s) && m.getParameterTypes().length == 1) {
                        methods.put(s, m);
                        return m;
                    }
                }
                methods.put(s, null);
                return null;
            }
        }
    }

    private Map<String, Entry> mapping = new HashMap<String, Entry>();
    private DefaultHandler parser;

    /**
     * The queue of already parsed items from the parsing thread.
     */
    private List<Object> queue = new LinkedList<Object>();
    private Iterator<Object> queueIterator = null;

    public XmlObjectParser() {
        parser = new Parser();
    }

    public XmlObjectParser(DefaultHandler handler) {
        parser = handler;
    }

    private Iterable<Object> start(final Reader in, final ContentHandler contentHandler) throws SAXException, IOException {
        try {
            SAXParserFactory parserFactory = SAXParserFactory.newInstance();
            parserFactory.setNamespaceAware(true);
            SAXParser saxParser = parserFactory.newSAXParser();
            XMLReader reader = saxParser.getXMLReader();
            reader.setContentHandler(contentHandler);
            try {
                // Do not load external DTDs (fix #8191)
                reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            } catch (SAXException e) {
                // Exception very unlikely to happen, so no need to translate this
                Main.error("Cannot disable 'load-external-dtd' feature: "+e.getMessage());
            }
            reader.parse(new InputSource(in));
            queueIterator = queue.iterator();
            return this;
        } catch (ParserConfigurationException e) {
            // This should never happen ;-)
            throw new RuntimeException(e);
        }
    }

    public Iterable<Object> start(final Reader in) throws SAXException {
        try {
            return start(in, parser);
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    public Iterable<Object> startWithValidation(final Reader in, String namespace, String schemaSource) throws SAXException {
        try {
            SchemaFactory factory =  SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
            Schema schema = factory.newSchema(new StreamSource(new MirroredInputStream(schemaSource)));
            ValidatorHandler validator = schema.newValidatorHandler();
            validator.setContentHandler(parser);
            validator.setErrorHandler(parser);

            AddNamespaceFilter filter = new AddNamespaceFilter(namespace);
            filter.setContentHandler(validator);
            return start(in, filter);
        } catch(IOException e) {
            throw new SAXException(tr("Failed to load XML schema."), e);
        }
    }

    public void map(String tagName, Class<?> klass) {
        mapping.put(tagName, new Entry(klass,false,false));
    }

    public void mapOnStart(String tagName, Class<?> klass) {
        mapping.put(tagName, new Entry(klass,true,false));
    }

    public void mapBoth(String tagName, Class<?> klass) {
        mapping.put(tagName, new Entry(klass,false,true));
    }

    public Object next() {
        return queueIterator.next();
    }

    public boolean hasNext() {
        return queueIterator.hasNext();
    }

    @Override
    public Iterator<Object> iterator() {
        return queue.iterator();
    }
}
