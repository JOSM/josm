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

import org.openstreetmap.josm.actions.search.SearchAction;
import org.openstreetmap.josm.actions.search.SearchCompiler;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetItem;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetType;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.xml.sax.SAXException;

public class Roles extends TaggingPresetItem {

    public static class Role {
        public Set<TaggingPresetType> types;
        public String key;
        /** The text to display */
        public String text;
        /** The context used for translating {@link #text} */
        public String text_context;
        /** The localized version of {@link #text}. */
        public String locale_text;
        public SearchCompiler.Match memberExpression;

        public boolean required;
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

        public void setMember_expression(String member_expression) throws SAXException {
            try {
                final SearchAction.SearchSetting searchSetting = new SearchAction.SearchSetting();
                searchSetting.text = member_expression;
                searchSetting.caseSensitive = true;
                searchSetting.regexSearch = true;
                this.memberExpression = SearchCompiler.compile(searchSetting);
            } catch (SearchCompiler.ParseError ex) {
                throw new SAXException(tr("Illegal member expression: {0}", ex.getMessage()), ex);
            }
        }

        public void setCount(String count) {
            this.count = Long.parseLong(count);
        }

        /**
         * Return either argument, the highest possible value or the lowest allowed value
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
    }

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
}
