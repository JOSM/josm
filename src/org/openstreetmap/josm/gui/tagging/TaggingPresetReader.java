// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.preferences.map.TaggingPresetPreference;
import org.openstreetmap.josm.io.MirroredInputStream;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.XmlObjectParser;
import org.xml.sax.SAXException;

/**
 * The tagging presets reader.
 * @since 6068
 */
public final class TaggingPresetReader {

    private TaggingPresetReader() {
        // Hide default constructor for utils classes
    }
    
    private static File zipIcons = null;
    
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
    
    public static List<TaggingPreset> readAll(Reader in, boolean validate) throws SAXException {
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

        LinkedList<TaggingPreset> all = new LinkedList<TaggingPreset>();
        TaggingPresetMenu lastmenu = null;
        TaggingPresetItems.Roles lastrole = null;
        final List<TaggingPresetItems.Check> checks = new LinkedList<TaggingPresetItems.Check>();
        List<TaggingPresetItems.PresetListEntry> listEntries = new LinkedList<TaggingPresetItems.PresetListEntry>();
        final Map<String, List<Object>> byId = new HashMap<String, List<Object>>();
        final Stack<String> lastIds = new Stack<String>();
        /** lastIdIterators contains non empty iterators of items to be handled before obtaining the next item from the XML parser */
        final Stack<Iterator<Object>> lastIdIterators = new Stack<Iterator<Object>>();

        if (validate) {
            parser.startWithValidation(in, Main.JOSM_WEBSITE+"/tagging-preset-1.0", "resource://data/tagging-preset.xsd");
        } else {
            parser.start(in);
        }
        while (parser.hasNext() || !lastIdIterators.isEmpty()) {
            final Object o;
            if (!lastIdIterators.isEmpty()) {
                // obtain elements from lastIdIterators with higher priority
                o = lastIdIterators.peek().next();
                if (!lastIdIterators.peek().hasNext()) {
                    // remove iterator is is empty
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
                    byId.put(lastId, new ArrayList<Object>());
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
                lastIdIterators.push(byId.get(ref).iterator());
                continue;
            }
            if (!(o instanceof TaggingPresetItem) && !checks.isEmpty()) {
                all.getLast().data.addAll(checks);
                checks.clear();
            }
            if (o instanceof TaggingPresetMenu) {
                TaggingPresetMenu tp = (TaggingPresetMenu) o;
                if (tp == lastmenu) {
                    lastmenu = tp.group;
                } else {
                    tp.group = lastmenu;
                    tp.setDisplayName();
                    lastmenu = tp;
                    all.add(tp);
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
                        listEntries = new LinkedList<TaggingPresetItems.PresetListEntry>();
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
    
    public static Collection<TaggingPreset> readAll(String source, boolean validate) throws SAXException, IOException {
        Collection<TaggingPreset> tp;
        MirroredInputStream s = new MirroredInputStream(source);
        try {
            InputStream zip = s.findZipEntryInputStream("xml","preset");
            if(zip != null) {
                zipIcons = s.getFile();
            }
            InputStreamReader r = new InputStreamReader(zip == null ? s : zip, Utils.UTF_8);
            try {
                tp = readAll(new BufferedReader(r), validate);
            } finally {
                Utils.close(r);
            }
        } finally {
            Utils.close(s);
        }
        return tp;
    }

    public static Collection<TaggingPreset> readAll(Collection<String> sources, boolean validate) {
        LinkedList<TaggingPreset> allPresets = new LinkedList<TaggingPreset>();
        for(String source : sources)  {
            try {
                allPresets.addAll(readAll(source, validate));
            } catch (IOException e) {
                Main.error(e);
                Main.error(source);
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("Could not read tagging preset source: {0}",source),
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE
                        );
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
    
    public static Collection<TaggingPreset> readFromPreferences(boolean validate) {
        return readAll(getPresetSources(), validate);
    }
    
    public static File getZipIcons() {
        return zipIcons;
    }
}
