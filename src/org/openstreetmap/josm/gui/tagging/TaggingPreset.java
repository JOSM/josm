// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.search.SearchCompiler;
import org.openstreetmap.josm.actions.search.SearchCompiler.Match;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditor;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.preferences.ToolbarPreferences;
import org.openstreetmap.josm.gui.preferences.map.TaggingPresetPreference;
import org.openstreetmap.josm.gui.tagging.TaggingPresetItems.Link;
import org.openstreetmap.josm.gui.tagging.TaggingPresetItems.Role;
import org.openstreetmap.josm.gui.tagging.TaggingPresetItems.Roles;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Predicate;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.template_engine.ParseError;
import org.openstreetmap.josm.tools.template_engine.TemplateEntry;
import org.openstreetmap.josm.tools.template_engine.TemplateParser;
import org.xml.sax.SAXException;

/**
 * This class read encapsulate one tagging preset. A class method can
 * read in all predefined presets, either shipped with JOSM or that are
 * in the config directory.
 *
 * It is also able to construct dialogs out of preset definitions.
 * @since 294
 */
public class TaggingPreset extends AbstractAction implements MapView.LayerChangeListener, Predicate<OsmPrimitive> {

    public static final int DIALOG_ANSWER_APPLY = 1;
    public static final int DIALOG_ANSWER_NEW_RELATION = 2;
    public static final int DIALOG_ANSWER_CANCEL = 3;

    public TaggingPresetMenu group = null;
    public String name;
    public String name_context;
    public String locale_name;
    public boolean preset_name_label;
    public final static String OPTIONAL_TOOLTIP_TEXT = "Optional tooltip text";

    /**
     * The types as preparsed collection.
     */
    public EnumSet<TaggingPresetType> types;
    public List<TaggingPresetItem> data = new LinkedList<TaggingPresetItem>();
    public Roles roles;
    public TemplateEntry nameTemplate;
    public Match nameTemplateFilter;

    /**
     * Create an empty tagging preset. This will not have any items and
     * will be an empty string as text. createPanel will return null.
     * Use this as default item for "do not select anything".
     */
    public TaggingPreset() {
        MapView.addLayerChangeListener(this);
        updateEnabledState();
    }

    /**
     * Change the display name without changing the toolbar value.
     */
    public void setDisplayName() {
        putValue(Action.NAME, getName());
        putValue("toolbar", "tagging_" + getRawName());
        putValue(OPTIONAL_TOOLTIP_TEXT, (group != null ?
                tr("Use preset ''{0}'' of group ''{1}''", getLocaleName(), group.getName()) :
                    tr("Use preset ''{0}''", getLocaleName())));
    }

    public String getLocaleName() {
        if(locale_name == null) {
            if(name_context != null) {
                locale_name = trc(name_context, TaggingPresetItems.fixPresetString(name));
            } else {
                locale_name = tr(TaggingPresetItems.fixPresetString(name));
            }
        }
        return locale_name;
    }

    public String getName() {
        return group != null ? group.getName() + "/" + getLocaleName() : getLocaleName();
    }
    public String getRawName() {
        return group != null ? group.getRawName() + "/" + name : name;
    }
    
    /**
     * Returns the preset icon.
     * @return The preset icon, or {@code null} if none defined
     * @since 6403
     */
    public final ImageIcon getIcon() {
        Object icon = getValue(Action.SMALL_ICON);
        if (icon instanceof ImageIcon) {
            return (ImageIcon) icon;
        }
        return null;
    }

    /**
     * Called from the XML parser to set the icon.
     * This task is performed in the background in order to speedup startup.
     *
     * FIXME for Java 1.6 - use 24x24 icons for LARGE_ICON_KEY (button bar)
     * and the 16x16 icons for SMALL_ICON.
     */
    public void setIcon(final String iconName) {
        ImageProvider imgProv = new ImageProvider(iconName);
        final Collection<String> s = Main.pref.getCollection("taggingpreset.icon.sources", null);
        imgProv.setDirs(s);
        imgProv.setId("presets");
        imgProv.setArchive(TaggingPresetReader.getZipIcons());
        imgProv.setOptional(true);
        imgProv.setMaxWidth(16).setMaxHeight(16);
        imgProv.getInBackground(new ImageProvider.ImageCallback() {
            @Override
            public void finished(final ImageIcon result) {
                if (result != null) {
                    GuiHelper.runInEDT(new Runnable() {
                        @Override
                        public void run() {
                            putValue(Action.SMALL_ICON, result);
                        }
                    });
                } else {
                    Main.warn("Could not get presets icon " + iconName);
                }
            }
        });
    }

    /**
     * Called from the XML parser to set the types this preset affects.
     */
    public void setType(String types) throws SAXException {
        this.types = TaggingPresetItems.getType(types);
    }

    public void setName_template(String pattern) throws SAXException {
        try {
            this.nameTemplate = new TemplateParser(pattern).parse();
        } catch (ParseError e) {
            Main.error("Error while parsing " + pattern + ": " + e.getMessage());
            throw new SAXException(e);
        }
    }

    public void setName_template_filter(String filter) throws SAXException {
        try {
            this.nameTemplateFilter = SearchCompiler.compile(filter, false, false);
        } catch (org.openstreetmap.josm.actions.search.SearchCompiler.ParseError e) {
            Main.error("Error while parsing" + filter + ": " + e.getMessage());
            throw new SAXException(e);
        }
    }

    private static class PresetPanel extends JPanel {
        boolean hasElements = false;
        PresetPanel()
        {
            super(new GridBagLayout());
        }
    }

    public PresetPanel createPanel(Collection<OsmPrimitive> selected) {
        if (data == null)
            return null;
        PresetPanel p = new PresetPanel();
        LinkedList<TaggingPresetItem> l = new LinkedList<TaggingPresetItem>();
        LinkedList<TaggingPresetItem> presetLink = new LinkedList<TaggingPresetItem>();
        if(types != null){
            JPanel pp = new JPanel();
            for(TaggingPresetType t : types){
                JLabel la = new JLabel(ImageProvider.get(t.getIconName()));
                la.setToolTipText(tr("Elements of type {0} are supported.", tr(t.getName())));
                pp.add(la);
            }
            p.add(pp, GBC.eol());
        }
        if (preset_name_label) {
            TaggingPresetItems.Label.addLabel(p, getName());
        }

        boolean presetInitiallyMatches = !selected.isEmpty() && Utils.forAll(selected, this);
        JPanel items = new JPanel(new GridBagLayout());
        for (TaggingPresetItem i : data){
            if(i instanceof Link) {
                l.add(i);
            } else if (i instanceof TaggingPresetItems.PresetLink) {
                presetLink.add(i);
            } else {
                if(i.addToPanel(items, selected, presetInitiallyMatches)) {
                    p.hasElements = true;
                }
            }
        }
        p.add(items, GBC.eol().fill());
        if (selected.isEmpty() && !supportsRelation()) {
            GuiHelper.setEnabledRec(items, false);
        }

        // add PresetLink
        if (!presetLink.isEmpty()) {
            p.add(new JLabel(tr("Edit also …")), GBC.eol().insets(0, 8, 0, 0));
            for(TaggingPresetItem link : presetLink) {
                link.addToPanel(p, selected, presetInitiallyMatches);
            }
        }

        // add Link
        for(TaggingPresetItem link : l) {
            link.addToPanel(p, selected, presetInitiallyMatches);
        }
        
        // "Add toolbar button"
        JToggleButton tb = new JToggleButton(new ToolbarButtonAction());
        tb.setFocusable(false);
        p.add(tb, GBC.std(0,0).anchor(GBC.LINE_END));
        return p;
    }

    public boolean isShowable()
    {
        for(TaggingPresetItem i : data)
        {
            if(!(i instanceof TaggingPresetItems.Optional || i instanceof TaggingPresetItems.Space || i instanceof TaggingPresetItems.Key))
                return true;
        }
        return false;
    }

    public String suggestRoleForOsmPrimitive(OsmPrimitive osm) {
        if (roles != null && osm != null) {
            for (Role i : roles.roles) {
                if (i.memberExpression != null && i.memberExpression.match(osm)
                        && (i.types == null || i.types.isEmpty() || i.types.contains(TaggingPresetType.forPrimitive(osm)) )) {
                    return i.key;
                }
            }
        }
        return null;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (Main.main == null) return;
        if (Main.main.getCurrentDataSet() == null) return;

        Collection<OsmPrimitive> sel = createSelection(Main.main.getCurrentDataSet().getSelected());
        int answer = showDialog(sel, supportsRelation());

        if (!sel.isEmpty() && answer == DIALOG_ANSWER_APPLY) {
            Command cmd = createCommand(sel, getChangedTags());
            if (cmd != null) {
                Main.main.undoRedo.add(cmd);
            }
        } else if (answer == DIALOG_ANSWER_NEW_RELATION) {
            final Relation r = new Relation();
            final Collection<RelationMember> members = new HashSet<RelationMember>();
            for(Tag t : getChangedTags()) {
                r.put(t.getKey(), t.getValue());
            }
            for (OsmPrimitive osm : Main.main.getCurrentDataSet().getSelected()) {
                String role = suggestRoleForOsmPrimitive(osm);
                RelationMember rm = new RelationMember(role == null ? "" : role, osm);
                r.addMember(rm);
                members.add(rm);
            }
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    RelationEditor.getEditor(Main.main.getEditLayer(), r, members).setVisible(true);
                }
            });
        }
        Main.main.getCurrentDataSet().setSelected(Main.main.getCurrentDataSet().getSelected()); // force update

    }

    public int showDialog(Collection<OsmPrimitive> sel, final boolean showNewRelation) {
        PresetPanel p = createPanel(sel);
        if (p == null)
            return DIALOG_ANSWER_CANCEL;

        int answer = 1;
        if (p.getComponentCount() != 0 && (sel.isEmpty() || p.hasElements)) {
            String title = trn("Change {0} object", "Change {0} objects", sel.size(), sel.size());
            if(sel.isEmpty()) {
                if(originalSelectionEmpty) {
                    title = tr("Nothing selected!");
                } else {
                    title = tr("Selection unsuitable!");
                }
            }

            class PresetDialog extends ExtendedDialog {
                public PresetDialog(Component content, String title, ImageIcon icon, boolean disableApply) {
                    super(Main.parent,
                            title,
                            showNewRelation?
                                    new String[] { tr("Apply Preset"), tr("New relation"), tr("Cancel") }:
                                        new String[] { tr("Apply Preset"), tr("Cancel") },
                                        true);
                    if (icon != null)
                        setIconImage(icon.getImage());
                    contentInsets = new Insets(10,5,0,5);
                    if (showNewRelation) {
                        setButtonIcons(new String[] {"ok.png", "dialogs/addrelation.png", "cancel.png" });
                    } else {
                        setButtonIcons(new String[] {"ok.png", "cancel.png" });
                    }
                    setContent(content);
                    setDefaultButton(1);
                    setupDialog();
                    buttons.get(0).setEnabled(!disableApply);
                    buttons.get(0).setToolTipText(title);
                    // Prevent dialogs of being too narrow (fix #6261)
                    Dimension d = getSize();
                    if (d.width < 350) {
                        d.width = 350;
                        setSize(d);
                    }
                    showDialog();
                }
            }

            answer = new PresetDialog(p, title, (ImageIcon) getValue(Action.SMALL_ICON), sel.isEmpty()).getValue();
        }
        if (!showNewRelation && answer == 2)
            return DIALOG_ANSWER_CANCEL;
        else
            return answer;
    }

    /**
     * True whenever the original selection given into createSelection was empty
     */
    private boolean originalSelectionEmpty = false;

    /**
     * Removes all unsuitable OsmPrimitives from the given list
     * @param participants List of possible OsmPrimitives to tag
     * @return Cleaned list with suitable OsmPrimitives only
     */
    public Collection<OsmPrimitive> createSelection(Collection<OsmPrimitive> participants) {
        originalSelectionEmpty = participants.isEmpty();
        Collection<OsmPrimitive> sel = new LinkedList<OsmPrimitive>();
        for (OsmPrimitive osm : participants)
        {
            if (types != null)
            {
                if(osm instanceof Relation)
                {
                    if(!types.contains(TaggingPresetType.RELATION) &&
                            !(types.contains(TaggingPresetType.CLOSEDWAY) && ((Relation)osm).isMultipolygon())) {
                        continue;
                    }
                }
                else if(osm instanceof Node)
                {
                    if(!types.contains(TaggingPresetType.NODE)) {
                        continue;
                    }
                }
                else if(osm instanceof Way)
                {
                    if(!types.contains(TaggingPresetType.WAY) &&
                            !(types.contains(TaggingPresetType.CLOSEDWAY) && ((Way)osm).isClosed())) {
                        continue;
                    }
                }
            }
            sel.add(osm);
        }
        return sel;
    }

    public List<Tag> getChangedTags() {
        List<Tag> result = new ArrayList<Tag>();
        for (TaggingPresetItem i: data) {
            i.addCommands(result);
        }
        return result;
    }

    public static Command createCommand(Collection<OsmPrimitive> sel, List<Tag> changedTags) {
        List<Command> cmds = new ArrayList<Command>();
        for (Tag tag: changedTags) {
            cmds.add(new ChangePropertyCommand(sel, tag.getKey(), tag.getValue()));
        }

        if (cmds.size() == 0)
            return null;
        else if (cmds.size() == 1)
            return cmds.get(0);
        else
            return new SequenceCommand(tr("Change Tags"), cmds);
    }

    private boolean supportsRelation() {
        return types == null || types.contains(TaggingPresetType.RELATION);
    }

    protected void updateEnabledState() {
        setEnabled(Main.main != null && Main.main.getCurrentDataSet() != null);
    }

    @Override
    public void activeLayerChange(Layer oldLayer, Layer newLayer) {
        updateEnabledState();
    }

    @Override
    public void layerAdded(Layer newLayer) {
        updateEnabledState();
    }

    @Override
    public void layerRemoved(Layer oldLayer) {
        updateEnabledState();
    }

    @Override
    public String toString() {
        return (types == null?"":types) + " " + name;
    }

    public boolean typeMatches(Collection<TaggingPresetType> t) {
        return t == null || types == null || types.containsAll(t);
    }

    @Override
    public boolean evaluate(OsmPrimitive p) {
        return matches(EnumSet.of(TaggingPresetType.forPrimitive(p)), p.getKeys(), false);
    }

    public boolean matches(Collection<TaggingPresetType> t, Map<String, String> tags, boolean onlyShowable) {
        if (onlyShowable && !isShowable())
            return false;
        else if (!typeMatches(t))
            return false;
        boolean atLeastOnePositiveMatch = false;
        for (TaggingPresetItem item : data) {
            Boolean m = item.matches(tags);
            if (m != null && !m)
                return false;
            else if (m != null) {
                atLeastOnePositiveMatch = true;
            }
        }
        return atLeastOnePositiveMatch;
    }

    public static Collection<TaggingPreset> getMatchingPresets(final Collection<TaggingPresetType> t, final Map<String, String> tags, final boolean onlyShowable) {
        return Utils.filter(TaggingPresetPreference.taggingPresets, new Predicate<TaggingPreset>() {
            @Override
            public boolean evaluate(TaggingPreset object) {
                return object.matches(t, tags, onlyShowable);
            }
        });
    }
    
    /**
     * Action that adds or removes the button on main toolbar
     */
    public class ToolbarButtonAction extends AbstractAction {
        private final int toolbarIndex;
        public ToolbarButtonAction() {
            super("", ImageProvider.get("styles/standard/waypoint","pin"));
            putValue(SHORT_DESCRIPTION, tr("Add or remove toolbar button"));
            LinkedList<String> t = new LinkedList<String>(ToolbarPreferences.getToolString());
            toolbarIndex = t.indexOf(getToolbarString());
            putValue(SELECTED_KEY, toolbarIndex >= 0);
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            String res = getToolbarString();
            Main.toolbar.addCustomButton(res, toolbarIndex, true);
        }
    }
    
    public String getToolbarString() {
        ToolbarPreferences.ActionDefinition aDef
            = new ToolbarPreferences.ActionDefinition(this);
        ToolbarPreferences.ActionParser actionParser = new ToolbarPreferences.ActionParser(null);
        return actionParser.saveAction(aDef);
    }
}
