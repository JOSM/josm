// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipFile;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.data.preferences.sources.PresetPrefHelper;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.tagging.presets.items.Check;
import org.openstreetmap.josm.gui.tagging.presets.items.CheckGroup;
import org.openstreetmap.josm.gui.tagging.presets.items.Combo;
import org.openstreetmap.josm.gui.tagging.presets.items.ComboMultiSelect;
import org.openstreetmap.josm.gui.tagging.presets.items.ItemSeparator;
import org.openstreetmap.josm.gui.tagging.presets.items.Key;
import org.openstreetmap.josm.gui.tagging.presets.items.Label;
import org.openstreetmap.josm.gui.tagging.presets.items.Link;
import org.openstreetmap.josm.gui.tagging.presets.items.MultiSelect;
import org.openstreetmap.josm.gui.tagging.presets.items.Optional;
import org.openstreetmap.josm.gui.tagging.presets.items.PresetLink;
import org.openstreetmap.josm.gui.tagging.presets.items.PresetListEntry;
import org.openstreetmap.josm.gui.tagging.presets.items.Roles;
import org.openstreetmap.josm.gui.tagging.presets.items.Roles.Role;
import org.openstreetmap.josm.gui.tagging.presets.items.Space;
import org.openstreetmap.josm.gui.tagging.presets.items.Text;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.io.NetworkManager;
import org.openstreetmap.josm.io.UTFInputStreamReader;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Stopwatch;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.XmlObjectParser;
import org.xml.sax.SAXException;

/**
 * The tagging presets reader.
 * @since 6068
 */
public final class TaggingPresetReader {

    /**
     * The accepted MIME types sent in the HTTP Accept header.
     * @since 6867
     */
    public static final String PRESET_MIME_TYPES =
            "application/xml, text/xml, text/plain; q=0.8, application/zip, application/octet-stream; q=0.5";

    /**
     * The XML namespace for the tagging presets
     * @since 16640
     */
    public static final String NAMESPACE = Config.getUrls().getXMLBase() + "/tagging-preset-1.0";

    /**
     * The internal resource URL of the XML schema file to be used with {@link CachedFile}
     * @since 16640
     */
    public static final String SCHEMA_SOURCE = "resource://data/tagging-preset.xsd";

    private static volatile File zipIcons;
    private static volatile boolean loadIcons = true;

    /**
     * Holds a reference to a chunk of items/objects.
     */
    public static class Chunk {
        /** The chunk id, can be referenced later */
        public String id;

        @Override
        public String toString() {
            return "Chunk [id=" + id + ']';
        }
    }

    /**
     * Holds a reference to an earlier item/object.
     */
    public static class Reference {
        /** Reference matching a chunk id defined earlier **/
        public String ref;

        @Override
        public String toString() {
            return "Reference [ref=" + ref + ']';
        }
    }

    /**
     * A {@link LinkedHashSet} with the ability to get the "last" object.
     * Note that this is unnecessary in Java 21 (see JEP 431).
     * @param <E> The object type in the set
     */
    static class HashSetWithLast<E> extends LinkedHashSet<E> {
        private static final long serialVersionUID = 1L;
        protected transient E last;

        @Override
        public boolean add(E e) {
            last = e;
            return super.add(e);
        }

        /**
         * Returns the last inserted element.
         * @return the last inserted element
         */
        public E getLast() {
            return last;
        }
    }

    /**
     * Returns the set of preset source URLs.
     * @return The set of preset source URLs.
     */
    public static Set<String> getPresetSources() {
        return new PresetPrefHelper().getActiveUrls();
    }

    private static XmlObjectParser buildParser() {
        XmlObjectParser parser = new XmlObjectParser();
        parser.mapOnStart("item", TaggingPreset.class);
        parser.mapOnStart("separator", TaggingPresetSeparator.class);
        parser.mapBoth("group", TaggingPresetMenu.class);
        parser.map("text", Text.class);
        parser.map("link", Link.class);
        parser.map("preset_link", PresetLink.class);
        parser.mapOnStart("optional", Optional.class);
        parser.mapOnStart("roles", Roles.class);
        parser.map("role", Role.class);
        parser.mapBoth("checkgroup", CheckGroup.class);
        parser.map("check", Check.class);
        parser.map("combo", Combo.class);
        parser.map("multiselect", MultiSelect.class);
        parser.map("label", Label.class);
        parser.map("space", Space.class);
        parser.map("key", Key.class);
        parser.map("list_entry", PresetListEntry.class);
        parser.map("item_separator", ItemSeparator.class);
        parser.mapBoth("chunk", Chunk.class);
        parser.map("reference", Reference.class);
        return parser;
    }

    /**
     * Reads all tagging presets from the input reader.
     * @param in The input reader
     * @param validate if {@code true}, XML validation will be performed
     * @return collection of tagging presets
     * @throws SAXException if any XML error occurs
     */
    public static Collection<TaggingPreset> readAll(Reader in, boolean validate) throws SAXException {
        return readAll(in, validate, new HashSetWithLast<>());
    }

    /**
     * Reads all tagging presets from the input reader.
     * @param in The input reader
     * @param validate if {@code true}, XML validation will be performed
     * @param all the accumulator for parsed tagging presets
     * @return the accumulator
     * @throws SAXException if any XML error occurs
     */
    static Collection<TaggingPreset> readAll(Reader in, boolean validate, HashSetWithLast<TaggingPreset> all) throws SAXException {
        XmlObjectParser parser = buildParser();

        /* to detect end of {@code <checkgroup>} */
        CheckGroup lastcheckgroup = null;
        /* to detect end of {@code <group>} */
        TaggingPresetMenu lastmenu = null;
        /* to detect end of reused {@code <group>} */
        TaggingPresetMenu lastmenuOriginal = null;
        Roles lastrole = null;
        final List<Check> checks = new LinkedList<>();
        final List<PresetListEntry> listEntries = new LinkedList<>();
        final Map<String, List<Object>> byId = new HashMap<>();
        final Deque<String> lastIds = new ArrayDeque<>();
        /* lastIdIterators contains non empty iterators of items to be handled before obtaining the next item from the XML parser */
        final Deque<Iterator<Object>> lastIdIterators = new ArrayDeque<>();

        if (validate) {
            parser.startWithValidation(in, NAMESPACE, SCHEMA_SOURCE);
        } else {
            parser.start(in);
        }
        while (parser.hasNext() || !lastIdIterators.isEmpty()) {
            final Object o;
            if (!lastIdIterators.isEmpty()) {
                // obtain elements from lastIdIterators with higher priority
                o = lastIdIterators.peek().next();
                if (!lastIdIterators.peek().hasNext()) {
                    // remove iterator if is empty
                    lastIdIterators.pop();
                }
            } else {
                o = parser.next();
            }
            Logging.trace("Preset object: {0}", o);
            if (o instanceof Chunk) {
                if (!lastIds.isEmpty() && ((Chunk) o).id.equals(lastIds.peek())) {
                    // pop last id on end of object, don't process further
                    lastIds.pop();
                    ((Chunk) o).id = null;
                } else {
                    // if preset item contains an id, store a mapping for later usage
                    String lastId = ((Chunk) o).id;
                    lastIds.push(lastId);
                    byId.put(lastId, new ArrayList<>());
                }
                continue;
            } else if (!lastIds.isEmpty()) {
                // add object to mapping for later usage
                byId.get(lastIds.peek()).add(o);
                continue;
            }
            if (o instanceof Reference) {
                // if o is a reference, obtain the corresponding objects from the mapping,
                // and iterate over those before consuming the next element from parser.
                final String ref = ((Reference) o).ref;
                if (byId.get(ref) == null) {
                    throw new SAXException(tr("Reference {0} is being used before it was defined", ref));
                }
                Iterator<Object> it = byId.get(ref).iterator();
                if (it.hasNext()) {
                    lastIdIterators.push(it);
                    if (lastIdIterators.size() > 100) {
                        throw new SAXException(tr("Reference stack for {0} is too large", ref));
                    }
                } else {
                    Logging.warn("Ignoring reference '"+ref+"' denoting an empty chunk");
                }
                continue;
            }
            if (!(o instanceof TaggingPresetItem) && !checks.isEmpty()) {
                all.getLast().data.addAll(checks);
                checks.clear();
            }
            if (o instanceof TaggingPresetMenu) {
                TaggingPresetMenu tp = (TaggingPresetMenu) o;
                if (tp == lastmenu || tp == lastmenuOriginal) {
                    lastmenu = tp.group;
                } else {
                    tp.group = lastmenu;
                    if (all.contains(tp)) {
                        lastmenuOriginal = tp;
                        tp = (TaggingPresetMenu) all.stream().filter(tp::equals).findFirst().orElse(tp);
                        lastmenuOriginal.group = null;
                    } else {
                        tp.setDisplayName();
                        all.add(tp);
                        lastmenuOriginal = null;
                    }
                    lastmenu = tp;
                }
                lastrole = null;
            } else if (o instanceof TaggingPresetSeparator) {
                TaggingPresetSeparator tp = (TaggingPresetSeparator) o;
                tp.group = lastmenu;
                all.add(tp);
                lastrole = null;
            } else if (o instanceof TaggingPreset) {
                TaggingPreset tp = (TaggingPreset) o;
                tp.group = lastmenu;
                tp.setDisplayName();
                all.add(tp);
                lastrole = null;
            } else {
                if (!all.isEmpty()) {
                    if (o instanceof Roles) {
                        all.getLast().data.add((TaggingPresetItem) o);
                        if (all.getLast().roles != null) {
                            throw new SAXException(tr("Roles cannot appear more than once"));
                        }
                        all.getLast().roles = (Roles) o;
                        lastrole = (Roles) o;
                        // #16458 - Make sure we don't duplicate role entries if used in a chunk/reference
                        lastrole.roles.clear();
                    } else if (o instanceof Role) {
                        if (lastrole == null)
                            throw new SAXException(tr("Preset role element without parent"));
                        lastrole.roles.add((Role) o);
                    } else if (o instanceof Check) {
                        if (lastcheckgroup != null) {
                            checks.add((Check) o);
                        } else {
                            all.getLast().data.add((TaggingPresetItem) o);
                        }
                    } else if (o instanceof PresetListEntry) {
                        listEntries.add((PresetListEntry) o);
                    } else if (o instanceof CheckGroup) {
                        CheckGroup cg = (CheckGroup) o;
                        if (cg == lastcheckgroup) {
                            lastcheckgroup = null;
                            all.getLast().data.add(cg);
                            // Make sure list of checks is empty to avoid adding checks several times
                            // when used in chunks (fix #10801)
                            cg.checks.clear();
                            cg.checks.addAll(checks);
                            checks.clear();
                        } else {
                            lastcheckgroup = cg;
                        }
                    } else {
                        if (!checks.isEmpty()) {
                            all.getLast().data.addAll(checks);
                            checks.clear();
                        }
                        all.getLast().data.add((TaggingPresetItem) o);
                        if (o instanceof ComboMultiSelect) {
                            ((ComboMultiSelect) o).addListEntries(listEntries);
                        } else if (o instanceof Key && ((Key) o).value == null) {
                            ((Key) o).value = ""; // Fix #8530
                        }
                        listEntries.clear();
                        lastrole = null;
                    }
                } else
                    throw new SAXException(tr("Preset sub element without parent"));
            }
        }
        if (!all.isEmpty() && !checks.isEmpty()) {
            all.getLast().data.addAll(checks);
            checks.clear();
        }
        return all;
    }

    /**
     * Reads all tagging presets from the given source.
     * @param source a given filename, URL or internal resource
     * @param validate if {@code true}, XML validation will be performed
     * @return collection of tagging presets
     * @throws SAXException if any XML error occurs
     * @throws IOException if any I/O error occurs
     */
    public static Collection<TaggingPreset> readAll(String source, boolean validate) throws SAXException, IOException {
        return readAll(source, validate, new HashSetWithLast<>());
    }

    /**
     * Reads all tagging presets from the given source.
     * @param source a given filename, URL or internal resource
     * @param validate if {@code true}, XML validation will be performed
     * @param all the accumulator for parsed tagging presets
     * @return the accumulator
     * @throws SAXException if any XML error occurs
     * @throws IOException if any I/O error occurs
     */
    static Collection<TaggingPreset> readAll(String source, boolean validate, HashSetWithLast<TaggingPreset> all)
            throws SAXException, IOException {
        Collection<TaggingPreset> tp;
        Logging.debug("Reading presets from {0}", source);
        Stopwatch stopwatch = Stopwatch.createStarted();
        try (
            CachedFile cf = new CachedFile(source).setHttpAccept(PRESET_MIME_TYPES);
        ) {
            Pair<ZipFile, InputStream> zip = cf.findZipEntryInputStream("xml", "preset");
            try {
                if (zip != null) {
                    zipIcons = cf.getFile();
                    I18n.addTexts(zipIcons);
                }
                try (InputStreamReader r = UTFInputStreamReader.create(zip == null ? cf.getInputStream() : zip.b)) {
                    tp = readAll(new BufferedReader(r), validate, all);
                }
            } finally {
                if (zip != null) {
                    Utils.close(zip.b);
                    Utils.close(zip.a);
                }
            }
        }
        Logging.debug(stopwatch.toString("Reading presets"));
        return tp;
    }

    /**
     * Reads all tagging presets from the given sources.
     * @param sources Collection of tagging presets sources.
     * @param validate if {@code true}, presets will be validated against XML schema
     * @return Collection of all presets successfully read
     */
    public static Collection<TaggingPreset> readAll(Collection<String> sources, boolean validate) {
        return readAll(sources, validate, true);
    }

    /**
     * Reads all tagging presets from the given sources.
     * @param sources Collection of tagging presets sources.
     * @param validate if {@code true}, presets will be validated against XML schema
     * @param displayErrMsg if {@code true}, a blocking error message is displayed in case of I/O exception.
     * @return Collection of all presets successfully read
     */
    public static Collection<TaggingPreset> readAll(Collection<String> sources, boolean validate, boolean displayErrMsg) {
        HashSetWithLast<TaggingPreset> allPresets = new HashSetWithLast<>();
        for (String source : sources) {
            try {
                readAll(source, validate, allPresets);
            } catch (IOException e) {
                Logging.log(Logging.LEVEL_ERROR, e);
                Logging.error(source);
                if (source.startsWith("http")) {
                    NetworkManager.addNetworkError(source, e);
                }
                if (displayErrMsg) {
                    JOptionPane.showMessageDialog(
                            MainApplication.getMainFrame(),
                            tr("Could not read tagging preset source: {0}", source),
                            tr("Error"),
                            JOptionPane.ERROR_MESSAGE
                            );
                }
            } catch (SAXException | IllegalArgumentException e) {
                Logging.error(e);
                Logging.error(source);
                if (displayErrMsg) {
                    JOptionPane.showMessageDialog(
                            MainApplication.getMainFrame(),
                            "<html>" + tr("Error parsing {0}: ", source) + "<br><br><table width=600>" +
                                    Utils.escapeReservedCharactersHTML(e.getMessage()) + "</table></html>",
                            tr("Error"),
                            JOptionPane.ERROR_MESSAGE
                            );
                }
            }
        }
        return allPresets;
    }

    /**
     * Reads all tagging presets from sources stored in preferences.
     * @param validate if {@code true}, presets will be validated against XML schema
     * @param displayErrMsg if {@code true}, a blocking error message is displayed in case of I/O exception.
     * @return Collection of all presets successfully read
     */
    public static Collection<TaggingPreset> readFromPreferences(boolean validate, boolean displayErrMsg) {
        return readAll(getPresetSources(), validate, displayErrMsg);
    }

    /**
     * Returns the zip file where the icons are located
     * @return the zip file where the icons are located
     */
    public static File getZipIcons() {
        return zipIcons;
    }

    /**
     * Determines if icon images should be loaded.
     * @return {@code true} if icon images should be loaded
     */
    public static boolean isLoadIcons() {
        return loadIcons;
    }

    /**
     * Sets whether icon images should be loaded.
     * @param loadIcons {@code true} if icon images should be loaded
     */
    public static void setLoadIcons(boolean loadIcons) {
        TaggingPresetReader.loadIcons = loadIcons;
    }

    private TaggingPresetReader() {
        // Hide default constructor for utils classes
    }
}
