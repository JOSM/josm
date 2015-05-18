// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
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

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.ValidatorHandler;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.io.CachedFile;
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
        private Stack<Object> current = new Stack<>();
        private StringBuilder characters = new StringBuilder(64);

        private Locator locator;

        @Override
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        protected void throwException(Exception e) throws XmlParsingException {
            throw new XmlParsingException(e).rememberLocation(locator);
        }

        @Override
        public void startElement(String ns, String lname, String qname, Attributes a) throws SAXException {
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

        @Override
        public void endElement(String ns, String lname, String qname) throws SAXException {
            if (mapping.containsKey(qname) && !mapping.get(qname).onStart) {
                report();
            } else if (mapping.containsKey(qname) && characters != null && !current.isEmpty()) {
                setValue(mapping.get(qname), qname, characters.toString().trim());
                characters  = new StringBuilder(64);
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
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
                return Long.valueOf(value);
            else if (klass == Float.TYPE || klass == Double.TYPE)
                return Double.valueOf(value);
            return value;
        }

        private void setValue(Entry entry, String fieldName, String value) throws SAXException {
            CheckParameterUtil.ensureParameterNotNull(entry, "entry");
            if ("class".equals(fieldName) || "default".equals(fieldName) || "throw".equals(fieldName) || "new".equals(fieldName) || "null".equals(fieldName)) {
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
                    && !"0".equals(s)
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
        private Class<?> klass;
        private boolean onStart;
        private boolean both;
        private final Map<String, Field> fields = new HashMap<>();
        private final Map<String, Method> methods = new HashMap<>();

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

    private Map<String, Entry> mapping = new HashMap<>();
    private DefaultHandler parser;

    /**
     * The queue of already parsed items from the parsing thread.
     */
    private List<Object> queue = new LinkedList<>();
    private Iterator<Object> queueIterator = null;

    /**
     * Constructs a new {@code XmlObjectParser}.
     */
    public XmlObjectParser() {
        parser = new Parser();
    }

    public XmlObjectParser(DefaultHandler handler) {
        parser = handler;
    }

    private Iterable<Object> start(final Reader in, final ContentHandler contentHandler) throws SAXException, IOException {
        try {
            XMLReader reader = Utils.newSafeSAXParser().getXMLReader();
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

    /**
     * Starts parsing from the given input reader, without validation.
     * @param in The input reader
     * @return iterable collection of objects
     * @throws SAXException if any XML or I/O error occurs
     */
    public Iterable<Object> start(final Reader in) throws SAXException {
        try {
            return start(in, parser);
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    /**
     * Starts parsing from the given input reader, with XSD validation.
     * @param in The input reader
     * @param namespace default namespace
     * @param schemaSource XSD schema
     * @return iterable collection of objects
     * @throws SAXException if any XML or I/O error occurs
     */
    public Iterable<Object> startWithValidation(final Reader in, String namespace, String schemaSource) throws SAXException {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try (InputStream mis = new CachedFile(schemaSource).getInputStream()) {
            Schema schema = factory.newSchema(new StreamSource(mis));
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
