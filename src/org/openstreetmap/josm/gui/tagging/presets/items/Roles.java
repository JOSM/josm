// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.search.SearchCompiler;
import org.openstreetmap.josm.data.osm.search.SearchParseError;
import org.openstreetmap.josm.data.osm.search.SearchSetting;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetItem;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetType;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.xml.sax.SAXException;

/**
 * The <code>roles</code> element in tagging presets definition.
 * <p>
 * A list of {@link Role} elements. Describes the roles that are expected for
 * the members of a relation.
 * <p>
 * Used for data validation, auto completion, among others.
 */
public class Roles extends TaggingPresetItem {

    /**
     * The <code>role</code> element in tagging preset definition.
     *
     * Information on a certain role, which is expected for the relation members.
     */
    public static class Role {
        public Set<TaggingPresetType> types; // NOSONAR
        /** Role name used in a relation */
        public String key; // NOSONAR
        /** Is the role name a regular expression */
        public boolean regexp; // NOSONAR
        /** The text to display */
        public String text; // NOSONAR
        /** The context used for translating {@link #text} */
        public String text_context; // NOSONAR
        /** The localized version of {@link #text}. */
        public String locale_text; // NOSONAR
        /** An expression (cf. search dialog) for objects of this role */
        public SearchCompiler.Match memberExpression; // NOSONAR
        /** Is this role required at least once in the relation? */
        public boolean required; // NOSONAR
        /** How often must the element appear */
        private long count;

        public void setType(String types) throws SAXException {
            this.types = getType(types);
        }

        public void setRequisite(String str) throws SAXException {
            if ("required".equals(str)) {
                required = true;
            } else if (!"optional".equals(str))
                throw new SAXException(tr("Unknown requisite: {0}", str));
        }

        public void setRegexp(String str) throws SAXException {
            if ("true".equals(str)) {
                regexp = true;
            } else if (!"false".equals(str))
                throw new SAXException(tr("Unknown regexp value: {0}", str));
        }

        public void setMember_expression(String memberExpression) throws SAXException {
            try {
                final SearchSetting searchSetting = new SearchSetting();
                searchSetting.text = memberExpression;
                searchSetting.caseSensitive = true;
                searchSetting.regexSearch = true;
                this.memberExpression = SearchCompiler.compile(searchSetting);
            } catch (SearchParseError ex) {
                throw new SAXException(tr("Illegal member expression: {0}", ex.getMessage()), ex);
            }
        }

        public void setCount(String count) {
            this.count = Long.parseLong(count);
        }

        /**
         * Return either argument, the highest possible value or the lowest allowed value
         * @param c count
         * @return the highest possible value or the lowest allowed value
         * @see #required
         */
        public long getValidCount(long c) {
            if (count > 0 && !required)
                return c != 0 ? count : 0;
            else if (count > 0)
                return count;
            else if (!required)
                return c != 0 ? c : 0;
            else
                return c != 0 ? c : 1;
        }

        /**
         * Check if the given role matches this class (required to check regexp role types)
         * @param role role to check
         * @return <code>true</code> if role matches
         * @since 11989
         */
        public boolean isRole(String role) {
            if (regexp && role != null) { // pass null through, it will anyway fail
                return role.matches(this.key);
            }
            return this.key.equals(role);
        }

        public boolean addToPanel(JPanel p) {
            String cstring;
            if (count > 0 && !required) {
                cstring = "0,"+count;
            } else if (count > 0) {
                cstring = String.valueOf(count);
            } else if (!required) {
                cstring = "0-...";
            } else {
                cstring = "1-...";
            }
            if (locale_text == null) {
                locale_text = getLocaleText(text, text_context, null);
            }
            p.add(new JLabel(locale_text+':'), GBC.std().insets(0, 0, 10, 0));
            p.add(new JLabel(key), GBC.std().insets(0, 0, 10, 0));
            p.add(new JLabel(cstring), types == null ? GBC.eol() : GBC.std().insets(0, 0, 10, 0));
            if (types != null) {
                JPanel pp = new JPanel();
                for (TaggingPresetType t : types) {
                    pp.add(new JLabel(ImageProvider.get(t.getIconName())));
                }
                p.add(pp, GBC.eol());
            }
            return true;
        }

        @Override
        public String toString() {
            return "Role [key=" + key + ", text=" + text + ']';
        }
    }

    /**
     * List of {@link Role} elements.
     */
    public final List<Role> roles = new LinkedList<>();

    @Override
    public boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel, boolean presetInitiallyMatches) {
        p.add(new JLabel(" "), GBC.eol()); // space
        if (!roles.isEmpty()) {
            JPanel proles = new JPanel(new GridBagLayout());
            proles.add(new JLabel(tr("Available roles")), GBC.std().insets(0, 0, 10, 0));
            proles.add(new JLabel(tr("role")), GBC.std().insets(0, 0, 10, 0));
            proles.add(new JLabel(tr("count")), GBC.std().insets(0, 0, 10, 0));
            proles.add(new JLabel(tr("elements")), GBC.eol());
            for (Role i : roles) {
                i.addToPanel(proles);
            }
            p.add(proles, GBC.eol());
        }
        return false;
    }

    @Override
    public void addCommands(List<Tag> changedTags) {
    }

    @Override
    public String toString() {
        return "Roles [roles=" + roles + ']';
    }
}
