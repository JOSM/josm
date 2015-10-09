// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging;

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

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.preferences.map.TaggingPresetPreference;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.io.UTFInputStreamReader;
import org.openstreetmap.josm.tools.Predicates;
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

    private TaggingPresetReader() {
        // Hide default constructor for utils classes
    }

    private static volatile File zipIcons;
    private static volatile boolean loadIcons = true;

    /**
     * Returns the set of preset source URLs.
     * @return The set of preset source URLs.
     */
    public static Set<String> getPresetSources() {
        return new TaggingPresetPreference.PresetPrefHelper().getActiveUrls();
    }

    /**
     * Holds a reference to a chunk of items/objects.
     */
    public static class Chunk {
        /** The chunk id, can be referenced later */
        public String id;
    }

    /**
     * Holds a reference to an earlier item/object.
     */
    public static class Reference {
        /** Reference matching a chunk id defined earlier **/
        public String ref;
    }

    private static XmlObjectParser buildParser() {
        XmlObjectParser parser = new XmlObjectParser();
        parser.mapOnStart("item", TaggingPreset.class);
        parser.mapOnStart("separator", TaggingPresetSeparator.class);
        parser.mapBoth("group", TaggingPresetMenu.class);
        parser.map("text", TaggingPresetItems.Text.class);
        parser.map("link", TaggingPresetItems.Link.class);
        parser.map("preset_link", TaggingPresetItems.PresetLink.class);
        parser.mapOnStart("optional", TaggingPresetItems.Optional.class);
        parser.mapOnStart("roles", TaggingPresetItems.Roles.class);
        parser.map("role", TaggingPresetItems.Role.class);
        parser.map("checkgroup", TaggingPresetItems.CheckGroup.class);
        parser.map("check", TaggingPresetItems.Check.class);
        parser.map("combo", TaggingPresetItems.Combo.class);
        parser.map("multiselect", TaggingPresetItems.MultiSelect.class);
        parser.map("label", TaggingPresetItems.Label.class);
        parser.map("space", TaggingPresetItems.Space.class);
        parser.map("key", TaggingPresetItems.Key.class);
        parser.map("list_entry", TaggingPresetItems.PresetListEntry.class);
        parser.map("item_separator", TaggingPresetItems.ItemSeparator.class);
        parser.mapBoth("chunk", Chunk.class);
        parser.map("reference", Reference.class);
        return parser;
    }

    static class HashSetWithLast<E> extends LinkedHashSet<E> {
        protected E last;

        @Override
        public boolean add(E e) {
            last = e;
            return super.add(e);
        }

        /**
         * Returns the last inserted element.
         */
        public E getLast() {
            return last;
        }
    }

    /**
     * Reads all tagging presets from the input reader.
     * @param in The input reader
     * @param validate if {@code true}, XML validation will be performed
     * @return collection of tagging presets
     * @throws SAXException if any XML error occurs
     */
    public static Collection<TaggingPreset> readAll(Reader in, boolean validate) throws SAXException {
        return readAll(in, validate, new HashSetWithLast<TaggingPreset>());
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

        /** to detect end of {@code <group>} */
        TaggingPresetMenu lastmenu = null;
        /** to detect end of reused {@code <group>} */
        TaggingPresetMenu lastmenuOriginal = null;
        TaggingPresetItems.Roles lastrole = null;
        final List<TaggingPresetItems.Check> checks = new LinkedList<>();
        List<TaggingPresetItems.PresetListEntry> listEntries = new LinkedList<>();
        final Map<String, List<Object>> byId = new HashMap<>();
        final Deque<String> lastIds = new ArrayDeque<>();
        /** lastIdIterators contains non empty iterators of items to be handled before obtaining the next item from the XML parser */
        final Deque<Iterator<Object>> lastIdIterators = new ArrayDeque<>();

        if (validate) {
            parser.startWithValidation(in, Main.getXMLBase()+"/tagging-preset-1.0", "resource://data/tagging-preset.xsd");
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
            if (o instanceof Chunk) {
                if (!lastIds.isEmpty() && ((Chunk) o).id.equals(lastIds.peek())) {
                    // pop last id on end of object, don't process further
                    lastIds.pop();
                    ((Chunk) o).id = null;
                    continue;
                } else {
                    // if preset item contains an id, store a mapping for later usage
                    String lastId = ((Chunk) o).id;
                    lastIds.push(lastId);
                    byId.put(lastId, new ArrayList<>());
                    continue;
                }
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
                } else {
                    Main.warn("Ignoring reference '"+ref+"' denoting an empty chunk");
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
                        tp = (TaggingPresetMenu) Utils.filter(all, Predicates.<TaggingPreset>equalTo(tp)).iterator().next();
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
                    if (o instanceof TaggingPresetItems.Roles) {
                        all.getLast().data.add((TaggingPresetItem) o);
                        if (all.getLast().roles != null) {
                            throw new SAXException(tr("Roles cannot appear more than once"));
                        }
                        all.getLast().roles = (TaggingPresetItems.Roles) o;
                        lastrole = (TaggingPresetItems.Roles) o;
                    } else if (o instanceof TaggingPresetItems.Role) {
                        if (lastrole == null)
                            throw new SAXException(tr("Preset role element without parent"));
                        lastrole.roles.add((TaggingPresetItems.Role) o);
                    } else if (o instanceof TaggingPresetItems.Check) {
                        checks.add((TaggingPresetItems.Check) o);
                    } else if (o instanceof TaggingPresetItems.PresetListEntry) {
                        listEntries.add((TaggingPresetItems.PresetListEntry) o);
                    } else if (o instanceof TaggingPresetItems.CheckGroup) {
                        all.getLast().data.add((TaggingPresetItem) o);
                        // Make sure list of checks is empty to avoid adding checks several times
                        // when used in chunks (fix #10801)
                        ((TaggingPresetItems.CheckGroup) o).checks.clear();
                        ((TaggingPresetItems.CheckGroup) o).checks.addAll(checks);
                        checks.clear();
                    } else {
                        if (!checks.isEmpty()) {
                            all.getLast().data.addAll(checks);
                            checks.clear();
                        }
                        all.getLast().data.add((TaggingPresetItem) o);
                        if (o instanceof TaggingPresetItems.ComboMultiSelect) {
                            ((TaggingPresetItems.ComboMultiSelect) o).addListEntries(listEntries);
                        } else if (o instanceof TaggingPresetItems.Key) {
                            if (((TaggingPresetItems.Key) o).value == null) {
                                ((TaggingPresetItems.Key) o).value = ""; // Fix #8530
                            }
                        }
                        listEntries = new LinkedList<>();
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
        return readAll(source, validate, new HashSetWithLast<TaggingPreset>());
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
        CachedFile cf = new CachedFile(source).setHttpAccept(PRESET_MIME_TYPES);
        try (
            // zip may be null, but Java 7 allows it: https://blogs.oracle.com/darcy/entry/project_coin_null_try_with
            InputStream zip = cf.findZipEntryInputStream("xml", "preset")
        ) {
            if (zip != null) {
                zipIcons = cf.getFile();
            }
            try (InputStreamReader r = UTFInputStreamReader.create(zip == null ? cf.getInputStream() : zip)) {
                tp = readAll(new BufferedReader(r), validate, all);
            }
        }
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
        for (String source : sources)  {
            try {
                readAll(source, validate, allPresets);
            } catch (IOException e) {
                Main.error(e, false);
                Main.error(source);
                if (source.startsWith("http")) {
                    Main.addNetworkError(source, e);
                }
                if (displayErrMsg) {
                    JOptionPane.showMessageDialog(
                            Main.parent,
                            tr("Could not read tagging preset source: {0}", source),
                            tr("Error"),
                            JOptionPane.ERROR_MESSAGE
                            );
                }
            } catch (SAXException e) {
                Main.error(e);
                Main.error(source);
                JOptionPane.showMessageDialog(
                        Main.parent,
                        "<html>" + tr("Error parsing {0}: ", source) + "<br><br><table width=600>" + e.getMessage() + "</table></html>",
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE
                        );
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

    public static File getZipIcons() {
        return zipIcons;
    }

    /**
     * Returns true if icon images should be loaded.
     */
    public static boolean isLoadIcons() {
        return loadIcons;
    }

    /**
     * Sets whether icon images should be loaded.
     */
    public static void setLoadIcons(boolean loadIcons) {
        TaggingPresetReader.loadIcons = loadIcons;
    }
}
