// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AdaptableAction;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.search.SearchCompiler;
import org.openstreetmap.josm.data.osm.search.SearchCompiler.Match;
import org.openstreetmap.josm.data.osm.search.SearchParseError;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditor;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeListener;
import org.openstreetmap.josm.gui.preferences.ToolbarPreferences;
import org.openstreetmap.josm.gui.tagging.presets.items.Key;
import org.openstreetmap.josm.gui.tagging.presets.items.Link;
import org.openstreetmap.josm.gui.tagging.presets.items.Optional;
import org.openstreetmap.josm.gui.tagging.presets.items.PresetLink;
import org.openstreetmap.josm.gui.tagging.presets.items.Roles;
import org.openstreetmap.josm.gui.tagging.presets.items.Roles.Role;
import org.openstreetmap.josm.gui.tagging.presets.items.Space;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
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
public class TaggingPreset extends AbstractAction implements ActiveLayerChangeListener, AdaptableAction, Predicate<OsmPrimitive> {

    public static final int DIALOG_ANSWER_APPLY = 1;
    public static final int DIALOG_ANSWER_NEW_RELATION = 2;
    public static final int DIALOG_ANSWER_CANCEL = 3;

    public static final String OPTIONAL_TOOLTIP_TEXT = "Optional tooltip text";

    /** Prefix of preset icon loading failure error message */
    public static final String PRESET_ICON_ERROR_MSG_PREFIX = "Could not get presets icon ";

    /**
     * The preset group this preset belongs to.
     */
    public TaggingPresetMenu group;

    /**
     * The name of the tagging preset.
     * @see #getRawName()
     */
    public String name;
    /**
     * The icon name assigned to this preset.
     */
    public String iconName;
    public String name_context;
    /**
     * A cache for the local name. Should never be accessed directly.
     * @see #getLocaleName()
     */
    public String locale_name;
    public boolean preset_name_label;

    /**
     * The types as preparsed collection.
     */
    public transient Set<TaggingPresetType> types;
    public final transient List<TaggingPresetItem> data = new LinkedList<>();
    public transient Roles roles;
    public transient TemplateEntry nameTemplate;
    public transient Match nameTemplateFilter;

    /**
     * True whenever the original selection given into createSelection was empty
     */
    private boolean originalSelectionEmpty;

    /**
     * Create an empty tagging preset. This will not have any items and
     * will be an empty string as text. createPanel will return null.
     * Use this as default item for "do not select anything".
     */
    public TaggingPreset() {
        MainApplication.getLayerManager().addActiveLayerChangeListener(this);
        updateEnabledState();
    }

    /**
     * Change the display name without changing the toolbar value.
     */
    public void setDisplayName() {
        putValue(Action.NAME, getName());
        putValue("toolbar", "tagging_" + getRawName());
        putValue(OPTIONAL_TOOLTIP_TEXT, group != null ?
                tr("Use preset ''{0}'' of group ''{1}''", getLocaleName(), group.getName()) :
                    tr("Use preset ''{0}''", getLocaleName()));
    }

    /**
     * Gets the localized version of the name
     * @return The name that should be displayed to the user.
     */
    public String getLocaleName() {
        if (locale_name == null) {
            if (name_context != null) {
                locale_name = trc(name_context, TaggingPresetItem.fixPresetString(name));
            } else {
                locale_name = tr(TaggingPresetItem.fixPresetString(name));
            }
        }
        return locale_name;
    }

    /**
     * Returns the translated name of this preset, prefixed with the group names it belongs to.
     * @return the translated name of this preset, prefixed with the group names it belongs to
     */
    public String getName() {
        return group != null ? group.getName() + '/' + getLocaleName() : getLocaleName();
    }

    /**
     * Returns the non translated name of this preset, prefixed with the (non translated) group names it belongs to.
     * @return the non translated name of this preset, prefixed with the (non translated) group names it belongs to
     */
    public String getRawName() {
        return group != null ? group.getRawName() + '/' + name : name;
    }

    /**
     * Returns the preset icon (16px).
     * @return The preset icon, or {@code null} if none defined
     * @since 6403
     */
    public final ImageIcon getIcon() {
        return getIcon(Action.SMALL_ICON);
    }

    /**
     * Returns the preset icon (16 or 24px).
     * @param key Key determining icon size: {@code Action.SMALL_ICON} for 16x, {@code Action.LARGE_ICON_KEY} for 24px
     * @return The preset icon, or {@code null} if none defined
     * @since 10849
     */
    public final ImageIcon getIcon(String key) {
        Object icon = getValue(key);
        if (icon instanceof ImageIcon) {
            return (ImageIcon) icon;
        }
        return null;
    }

    /**
     * Called from the XML parser to set the icon.
     * The loading task is performed in the background in order to speedup startup.
     * @param iconName icon name
     */
    public void setIcon(final String iconName) {
        this.iconName = iconName;
        if (!TaggingPresetReader.isLoadIcons()) {
            return;
        }
        File arch = TaggingPresetReader.getZipIcons();
        final Collection<String> s = Config.getPref().getList("taggingpreset.icon.sources", null);
        ImageProvider imgProv = new ImageProvider(iconName);
        imgProv.setDirs(s);
        imgProv.setId("presets");
        imgProv.setArchive(arch);
        imgProv.setOptional(true);
        imgProv.getResourceAsync().thenAccept(result -> {
            if (result != null) {
                GuiHelper.runInEDT(() -> result.attachImageIcon(this));
            } else {
                Logging.warn(toString() + ": " + PRESET_ICON_ERROR_MSG_PREFIX + iconName);
            }
        });
    }

    /**
     * Called from the XML parser to set the types this preset affects.
     * @param types comma-separated primitive types ("node", "way", "relation" or "closedway")
     * @throws SAXException if any SAX error occurs
     * @see TaggingPresetType#fromString
     */
    public void setType(String types) throws SAXException {
        this.types = TaggingPresetItem.getType(types);
    }

    public void setName_template(String pattern) throws SAXException {
        try {
            this.nameTemplate = new TemplateParser(pattern).parse();
        } catch (ParseError e) {
            Logging.error("Error while parsing " + pattern + ": " + e.getMessage());
            throw new SAXException(e);
        }
    }

    public void setName_template_filter(String filter) throws SAXException {
        try {
            this.nameTemplateFilter = SearchCompiler.compile(filter);
        } catch (SearchParseError e) {
            Logging.error("Error while parsing" + filter + ": " + e.getMessage());
            throw new SAXException(e);
        }
    }

    private static class PresetPanel extends JPanel {
        private boolean hasElements;

        PresetPanel() {
            super(new GridBagLayout());
        }
    }

    /**
     * Returns the tags being directly applied (without UI element) by {@link Key} items
     *
     * @return a list of tags
     */
    private List<Tag> getDirectlyAppliedTags() {
        List<Tag> tags = new ArrayList<>();
        for (TaggingPresetItem item : data) {
            if (item instanceof Key) {
                tags.add(((Key) item).asTag());
            }
        }
        return tags;
    }

    /**
     * Creates a panel for this preset. This includes general information such as name and supported {@link TaggingPresetType types}.
     * This includes the elements from the individual {@link TaggingPresetItem items}.
     *
     * @param selected the selected primitives
     * @return the newly created panel
     */
    public PresetPanel createPanel(Collection<OsmPrimitive> selected) {
        PresetPanel p = new PresetPanel();
        List<Link> l = new LinkedList<>();
        List<PresetLink> presetLink = new LinkedList<>();

        final JPanel pp = new JPanel();
        if (types != null) {
            for (TaggingPresetType t : types) {
                JLabel la = new JLabel(ImageProvider.get(t.getIconName()));
                la.setToolTipText(tr("Elements of type {0} are supported.", tr(t.getName())));
                pp.add(la);
            }
        }
        final List<Tag> directlyAppliedTags = getDirectlyAppliedTags();
        if (!directlyAppliedTags.isEmpty()) {
            final JLabel label = new JLabel(ImageProvider.get("pastetags"));
            label.setToolTipText("<html>" + tr("This preset also sets: {0}", Utils.joinAsHtmlUnorderedList(directlyAppliedTags)));
            pp.add(label);
        }
        final int count = pp.getComponentCount();
        if (preset_name_label) {
            p.add(new JLabel(getIcon(Action.LARGE_ICON_KEY)), GBC.std(0, 0).span(1, count > 0 ? 2 : 1).insets(0, 0, 5, 0));
        }
        if (count > 0) {
            p.add(pp, GBC.std(1, 0).span(GBC.REMAINDER));
        }
        if (preset_name_label) {
            p.add(new JLabel(getName()), GBC.std(1, count > 0 ? 1 : 0).insets(5, 0, 0, 0).span(GBC.REMAINDER).fill(GBC.HORIZONTAL));
        }

        boolean presetInitiallyMatches = !selected.isEmpty() && selected.stream().allMatch(this);
        JPanel items = new JPanel(new GridBagLayout());
        for (TaggingPresetItem i : data) {
            if (i instanceof Link) {
                l.add((Link) i);
                p.hasElements = true;
            } else if (i instanceof PresetLink) {
                presetLink.add((PresetLink) i);
            } else {
                if (i.addToPanel(items, selected, presetInitiallyMatches)) {
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
            for (PresetLink link : presetLink) {
                link.addToPanel(p, selected, presetInitiallyMatches);
            }
        }

        // add Link
        for (Link link : l) {
            link.addToPanel(p, selected, presetInitiallyMatches);
        }

        // "Add toolbar button"
        JToggleButton tb = new JToggleButton(new ToolbarButtonAction());
        tb.setFocusable(false);
        p.add(tb, GBC.std(1, 0).anchor(GBC.LINE_END));
        return p;
    }

    /**
     * Determines whether a dialog can be shown for this preset, i.e., at least one tag can/must be set by the user.
     *
     * @return {@code true} if a dialog can be shown for this preset
     */
    public boolean isShowable() {
        for (TaggingPresetItem i : data) {
            if (!(i instanceof Optional || i instanceof Space || i instanceof Key))
                return true;
        }
        return false;
    }

    public String suggestRoleForOsmPrimitive(OsmPrimitive osm) {
        if (roles != null && osm != null) {
            for (Role i : roles.roles) {
                if (i.memberExpression != null && i.memberExpression.match(osm)
                        && (i.types == null || i.types.isEmpty() || i.types.contains(TaggingPresetType.forPrimitive(osm)))) {
                    return i.key;
                }
            }
        }
        return null;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (Main.main == null) {
            return;
        }
        DataSet ds = Main.main.getEditDataSet();
        Collection<OsmPrimitive> participants = Collections.emptyList();
        if (ds != null) {
            participants = ds.getSelected();
        }

        // Display dialog even if no data layer (used by preset-tagging-tester plugin)
        Collection<OsmPrimitive> sel = createSelection(participants);
        int answer = showDialog(sel, supportsRelation());

        if (ds == null) {
            return;
        }

        if (!sel.isEmpty() && answer == DIALOG_ANSWER_APPLY) {
            Command cmd = createCommand(sel, getChangedTags());
            if (cmd != null) {
                MainApplication.undoRedo.add(cmd);
            }
        } else if (answer == DIALOG_ANSWER_NEW_RELATION) {
            final Relation r = new Relation();
            final Collection<RelationMember> members = new HashSet<>();
            for (Tag t : getChangedTags()) {
                r.put(t.getKey(), t.getValue());
            }
            for (OsmPrimitive osm : ds.getSelected()) {
                String role = suggestRoleForOsmPrimitive(osm);
                RelationMember rm = new RelationMember(role == null ? "" : role, osm);
                r.addMember(rm);
                members.add(rm);
            }
            SwingUtilities.invokeLater(() -> RelationEditor.getEditor(
                    MainApplication.getLayerManager().getEditLayer(), r, members).setVisible(true));
        }
        ds.setSelected(ds.getSelected()); // force update
    }

    private static class PresetDialog extends ExtendedDialog {

        /**
         * Constructs a new {@code PresetDialog}.
         * @param content the content that will be displayed in this dialog
         * @param title the text that will be shown in the window titlebar
         * @param icon the image to be displayed as the icon for this window
         * @param disableApply whether to disable "Apply" button
         * @param showNewRelation whether to display "New relation" button
         */
        PresetDialog(Component content, String title, ImageIcon icon, boolean disableApply, boolean showNewRelation) {
            super(Main.parent, title,
                    showNewRelation ?
                            (new String[] {tr("Apply Preset"), tr("New relation"), tr("Cancel")}) :
                            (new String[] {tr("Apply Preset"), tr("Cancel")}),
                    true);
            if (icon != null)
                setIconImage(icon.getImage());
            contentInsets = new Insets(10, 5, 0, 5);
            if (showNewRelation) {
                setButtonIcons("ok", "dialogs/addrelation", "cancel");
            } else {
                setButtonIcons("ok", "cancel");
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
            super.showDialog();
        }
    }

    /**
     * Shows the preset dialog.
     * @param sel selection
     * @param showNewRelation whether to display "New relation" button
     * @return the user choice after the dialog has been closed
     */
    public int showDialog(Collection<OsmPrimitive> sel, boolean showNewRelation) {
        PresetPanel p = createPanel(sel);

        int answer = 1;
        boolean canCreateRelation = types == null || types.contains(TaggingPresetType.RELATION);
        if (originalSelectionEmpty && !canCreateRelation) {
            new Notification(
                    tr("The preset <i>{0}</i> cannot be applied since nothing has been selected!", getLocaleName()))
                    .setIcon(JOptionPane.WARNING_MESSAGE)
                    .show();
            return DIALOG_ANSWER_CANCEL;
        } else if (sel.isEmpty() && !canCreateRelation) {
            new Notification(
                    tr("The preset <i>{0}</i> cannot be applied since the selection is unsuitable!", getLocaleName()))
                    .setIcon(JOptionPane.WARNING_MESSAGE)
                    .show();
            return DIALOG_ANSWER_CANCEL;
        } else if (p.getComponentCount() != 0 && (sel.isEmpty() || p.hasElements)) {
            String title = trn("Change {0} object", "Change {0} objects", sel.size(), sel.size());
            if (sel.isEmpty()) {
                if (originalSelectionEmpty) {
                    title = tr("Nothing selected!");
                } else {
                    title = tr("Selection unsuitable!");
                }
            }

            answer = new PresetDialog(p, title, preset_name_label ? null : (ImageIcon) getValue(Action.SMALL_ICON),
                    sel.isEmpty(), showNewRelation).getValue();
        }
        if (!showNewRelation && answer == 2)
            return DIALOG_ANSWER_CANCEL;
        else
            return answer;
    }

    /**
     * Removes all unsuitable OsmPrimitives from the given list
     * @param participants List of possible OsmPrimitives to tag
     * @return Cleaned list with suitable OsmPrimitives only
     */
    public Collection<OsmPrimitive> createSelection(Collection<OsmPrimitive> participants) {
        originalSelectionEmpty = participants.isEmpty();
        Collection<OsmPrimitive> sel = new LinkedList<>();
        for (OsmPrimitive osm : participants) {
            if (typeMatches(EnumSet.of(TaggingPresetType.forPrimitive(osm)))) {
                sel.add(osm);
            }
        }
        return sel;
    }

    /**
     * Gets a list of tags that are set by this preset.
     * @return The list of tags.
     */
    public List<Tag> getChangedTags() {
        List<Tag> result = new ArrayList<>();
        for (TaggingPresetItem i: data) {
            i.addCommands(result);
        }
        return result;
    }

    /**
     * Create a command to change the given list of tags.
     * @param sel The primitives to change the tags for
     * @param changedTags The tags to change
     * @return A command that changes the tags.
     */
    public static Command createCommand(Collection<OsmPrimitive> sel, List<Tag> changedTags) {
        List<Command> cmds = new ArrayList<>();
        for (Tag tag: changedTags) {
            ChangePropertyCommand cmd = new ChangePropertyCommand(sel, tag.getKey(), tag.getValue());
            if (cmd.getObjectsNumber() > 0) {
                cmds.add(cmd);
            }
        }

        if (cmds.isEmpty())
            return null;
        else if (cmds.size() == 1)
            return cmds.get(0);
        else
            return new SequenceCommand(tr("Change Tags"), cmds);
    }

    private boolean supportsRelation() {
        return types == null || types.contains(TaggingPresetType.RELATION);
    }

    protected final void updateEnabledState() {
        setEnabled(Main.main != null && Main.main.getEditDataSet() != null);
    }

    @Override
    public void activeOrEditLayerChanged(ActiveLayerChangeEvent e) {
        updateEnabledState();
    }

    @Override
    public String toString() {
        return (types == null ? "" : types.toString()) + ' ' + name;
    }

    /**
     * Determines whether this preset matches the types.
     * @param t The types that must match
     * @return <code>true</code> if all types match.
     */
    public boolean typeMatches(Collection<TaggingPresetType> t) {
        return t == null || types == null || types.containsAll(t);
    }

    /**
     * Determines whether this preset matches the given primitive, i.e.,
     * whether the {@link #typeMatches(Collection) type matches} and the {@link TaggingPresetItem#matches(Map) tags match}.
     *
     * @param p the primitive
     * @return {@code true} if this preset matches the primitive
     */
    @Override
    public boolean test(OsmPrimitive p) {
        return matches(EnumSet.of(TaggingPresetType.forPrimitive(p)), p.getKeys(), false);
    }

    /**
     * Determines whether this preset matches the parameters.
     *
     * @param t the preset types to include, see {@link #typeMatches(Collection)}
     * @param tags the tags to perform matching on, see {@link TaggingPresetItem#matches(Map)}
     * @param onlyShowable whether the preset must be {@link #isShowable() showable}
     * @return {@code true} if this preset matches the parameters.
     */
    public boolean matches(Collection<TaggingPresetType> t, Map<String, String> tags, boolean onlyShowable) {
        if ((onlyShowable && !isShowable()) || !typeMatches(t)) {
            return false;
        } else {
            return TaggingPresetItem.matches(data, tags);
        }
    }

    /**
     * Action that adds or removes the button on main toolbar
     */
    public class ToolbarButtonAction extends AbstractAction {
        private final int toolbarIndex;

        /**
         * Constructs a new {@code ToolbarButtonAction}.
         */
        public ToolbarButtonAction() {
            super("", ImageProvider.get("dialogs", "pin"));
            putValue(SHORT_DESCRIPTION, tr("Add or remove toolbar button"));
            List<String> t = new LinkedList<>(ToolbarPreferences.getToolString());
            toolbarIndex = t.indexOf(getToolbarString());
            putValue(SELECTED_KEY, toolbarIndex >= 0);
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            String res = getToolbarString();
            MainApplication.getToolbar().addCustomButton(res, toolbarIndex, true);
        }
    }

    /**
     * Gets a string describing this preset that can be used for the toolbar
     * @return A String that can be passed on to the toolbar
     * @see ToolbarPreferences#addCustomButton(String, int, boolean)
     */
    public String getToolbarString() {
        ToolbarPreferences.ActionParser actionParser = new ToolbarPreferences.ActionParser(null);
        return actionParser.saveAction(new ToolbarPreferences.ActionDefinition(this));
    }
}
