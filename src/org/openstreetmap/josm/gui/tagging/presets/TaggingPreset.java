// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.actions.AdaptableAction;
import org.openstreetmap.josm.actions.CreateMultipolygonAction;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.OsmData;
import org.openstreetmap.josm.data.osm.OsmDataManager;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.Tagged;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.search.SearchCompiler;
import org.openstreetmap.josm.data.osm.search.SearchCompiler.Match;
import org.openstreetmap.josm.data.osm.search.SearchParseError;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.sort.RelationSorter;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeListener;
import org.openstreetmap.josm.gui.preferences.ToolbarPreferences;
import org.openstreetmap.josm.gui.tagging.presets.items.Key;
import org.openstreetmap.josm.gui.tagging.presets.items.Link;
import org.openstreetmap.josm.gui.tagging.presets.items.Optional;
import org.openstreetmap.josm.gui.tagging.presets.items.PresetLink;
import org.openstreetmap.josm.gui.tagging.presets.items.Roles;
import org.openstreetmap.josm.gui.tagging.presets.items.Space;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ImageResource;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.StreamUtils;
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
public class TaggingPreset extends AbstractAction implements ActiveLayerChangeListener, AdaptableAction, Predicate<IPrimitive> {

    /** The user pressed the "Apply" button */
    public static final int DIALOG_ANSWER_APPLY = 1;
    /** The user pressed the "New Relation" button */
    public static final int DIALOG_ANSWER_NEW_RELATION = 2;
    /** The user pressed the "Cancel" button */
    public static final int DIALOG_ANSWER_CANCEL = 3;

    /** The action key for optional tooltips */
    public static final String OPTIONAL_TOOLTIP_TEXT = "Optional tooltip text";

    /** Prefix of preset icon loading failure error message */
    public static final String PRESET_ICON_ERROR_MSG_PREFIX = "Could not get presets icon ";

    /**
     * Defines whether the validator should be active in the preset dialog
     * @see TaggingPresetValidation
     */
    public static final BooleanProperty USE_VALIDATOR = new BooleanProperty("taggingpreset.validator", false);

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
    /**
     * Translation context for name
     */
    public String name_context;
    /**
     * A cache for the local name. Should never be accessed directly.
     * @see #getLocaleName()
     */
    public String locale_name;
    /**
     * Show the preset name if true
     */
    public boolean preset_name_label;

    /**
     * The types as preparsed collection.
     */
    public transient Set<TaggingPresetType> types;
    /**
     * list of regions the preset is applicable for
     */
    private Set<String> regions;
    /**
     * If true, invert the meaning of regions
     */
    private boolean exclude_regions;
    /**
     * The list of preset items
     */
    public final transient List<TaggingPresetItem> data = new ArrayList<>(2);
    /**
     * The roles for this relation (if we are editing a relation). See:
     * <a href="https://josm.openstreetmap.de/wiki/TaggingPresets#Tags">JOSM wiki</a>
     */
    public transient Roles roles;
    /**
     * The name_template custom name formatter. See:
     * <a href="https://josm.openstreetmap.de/wiki/TaggingPresets#Attributes">JOSM wiki</a>
     */
    public transient TemplateEntry nameTemplate;
    /** The name_template_filter */
    public transient Match nameTemplateFilter;
    /** The match_expression */
    public transient Match matchExpression;

    /**
     * True whenever the original selection given into createSelection was empty
     */
    private boolean originalSelectionEmpty;

    /** The completable future task of asynchronous icon loading */
    private CompletableFuture<Void> iconFuture;

    /** Support functions */
    protected TaggingPresetItemGuiSupport itemGuiSupport;

    /**
     * Create an empty tagging preset. This will not have any items and
     * will be an empty string as text. createPanel will return null.
     * Use this as default item for "do not select anything".
     */
    public TaggingPreset() {
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
     * Returns the {@link ImageResource} attached to this preset, if any.
     * @return the {@code ImageResource} attached to this preset, or {@code null}
     * @since 16060
     */
    public final ImageResource getImageResource() {
        return ImageResource.getAttachedImageResource(this);
    }

    /**
     * Called from the XML parser to set the icon.
     * The loading task is performed in the background in order to speedup startup.
     * @param iconName icon name
     */
    public void setIcon(final String iconName) {
        this.iconName = iconName;
        if (iconName == null || !TaggingPresetReader.isLoadIcons()) {
            return;
        }
        File arch = TaggingPresetReader.getZipIcons();
        final Collection<String> s = TaggingPresets.ICON_SOURCES.get();
        this.iconFuture = new CompletableFuture<>();
        new ImageProvider(iconName)
            .setDirs(s)
            .setId("presets")
            .setArchive(arch)
            .setOptional(true)
            .getResourceAsync(result -> {
                if (result != null) {
                    GuiHelper.runInEDT(() -> {
                        try {
                            result.attachImageIcon(this, true);
                        } catch (IllegalArgumentException e) {
                            Logging.warn(toString() + ": " + PRESET_ICON_ERROR_MSG_PREFIX + iconName);
                            Logging.warn(e);
                        } finally {
                            iconFuture.complete(null);
                        }
                    });
                } else {
                    Logging.warn(toString() + ": " + PRESET_ICON_ERROR_MSG_PREFIX + iconName);
                    iconFuture.complete(null);
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

    /**
     * Sets the name_template custom name formatter.
     *
     * @param template The format template
     * @throws SAXException on template parse error
     * @see <a href="https://josm.openstreetmap.de/wiki/TaggingPresets#name_templatedetails">JOSM wiki</a>
     */
    public void setName_template(String template) throws SAXException {
        try {
            this.nameTemplate = new TemplateParser(template).parse();
        } catch (ParseError e) {
            Logging.error("Error while parsing " + template + ": " + e.getMessage());
            throw new SAXException(e);
        }
    }

    /**
     * Sets the name_template_filter.
     *
     * @param filter The search pattern
     * @throws SAXException on search patern parse error
     * @see <a href="https://josm.openstreetmap.de/wiki/TaggingPresets#name_templatedetails">JOSM wiki</a>
     */
    public void setName_template_filter(String filter) throws SAXException {
        try {
            this.nameTemplateFilter = SearchCompiler.compile(filter);
        } catch (SearchParseError e) {
            Logging.error("Error while parsing" + filter + ": " + e.getMessage());
            throw new SAXException(e);
        }
    }

    /**
     * Sets the match_expression additional criteria for matching primitives.
     *
     * @param filter The search pattern
     * @throws SAXException on search patern parse error
     * @see <a href="https://josm.openstreetmap.de/wiki/TaggingPresets#Attributes">JOSM wiki</a>
     */
    public void setMatch_expression(String filter) throws SAXException {
        try {
            this.matchExpression = SearchCompiler.compile(filter);
        } catch (SearchParseError e) {
            Logging.error("Error while parsing" + filter + ": " + e.getMessage());
            throw new SAXException(e);
        }
    }

    /**
     * Get the regions for the preset
     * @return The regions that the preset is valid for
     * @apiNote This is not {@code getRegions} just in case we decide to make {@link TaggingPreset} a record class.
     * @since xxx
     */
    public final Set<String> regions() {
        return this.regions;
    }

    /**
     * Set the regions for the preset
     * @param regions The region list (comma delimited)
     * @since xxx
     */
    public final void setRegions(String regions) {
        this.regions = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(regions.split(","))));
    }

    /**
     * Get the exclude_regions for the preset
     * @apiNote This is not {@code getExclude_regions} just in case we decide to make {@link TaggingPreset} a record class.
     * @since xxx
     */
    public final boolean exclude_regions() {
        return this.exclude_regions;
    }

    /**
     * Set if the preset should not be used in the given region
     * @param excludeRegions if true the function of regions is inverted
     * @since xxx
     */
    public final void setExclude_regions(boolean excludeRegions) {
        this.exclude_regions = excludeRegions;
    }

    private static class PresetPanel extends JPanel {
        private boolean hasElements;

        PresetPanel() {
            super(new GridBagLayout());
        }
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

        final JPanel pp = new JPanel();
        if (types != null) {
            for (TaggingPresetType t : types) {
                JLabel la = new JLabel(ImageProvider.get(t.getIconName()));
                la.setToolTipText(tr("Elements of type {0} are supported.", tr(t.getName())));
                pp.add(la);
            }
        }
        final List<Tag> directlyAppliedTags = Utils.filteredCollection(data, Key.class).stream()
                .map(Key::asTag)
                .collect(Collectors.toList());
        if (!directlyAppliedTags.isEmpty()) {
            final JLabel label = new JLabel(ImageProvider.get("pastetags"));
            label.setToolTipText("<html>" + tr("This preset also sets: {0}", Utils.joinAsHtmlUnorderedList(directlyAppliedTags)));
            pp.add(label);
        }
        JLabel validationLabel = new JLabel(ImageProvider.get("warning-small", ImageProvider.ImageSizes.LARGEICON));
        validationLabel.setVisible(false);
        pp.add(validationLabel);

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
        itemGuiSupport = TaggingPresetItemGuiSupport.create(presetInitiallyMatches, selected, this::getChangedTags);

        JPanel itemPanel = new JPanel(new GridBagLayout()) {
            /**
             * This hack allows the items to have their own orientation.
             *
             * The problem is that
             * {@link org.openstreetmap.josm.gui.ExtendedDialog#showDialog ExtendedDialog} calls
             * {@code applyComponentOrientation} very late in the dialog construction process thus
             * overwriting the orientation the components have chosen for themselves.
             *
             * This stops the propagation of {@code applyComponentOrientation}, thus all
             * {@code TaggingPresetItem}s may (and have to) set their own orientation.
             */
            @Override
            public void applyComponentOrientation(ComponentOrientation o) {
                setComponentOrientation(o);
            }
        };
        JPanel linkPanel = new JPanel(new GridBagLayout());
        TaggingPresetItem previous = null;
        for (TaggingPresetItem i : data) {
            if (i instanceof Link) {
                i.addToPanel(linkPanel, itemGuiSupport);
                p.hasElements = true;
            } else {
                if (i instanceof PresetLink) {
                    PresetLink link = (PresetLink) i;
                    if (!(previous instanceof PresetLink && Objects.equals(((PresetLink) previous).text, link.text))) {
                        itemPanel.add(link.createLabel(), GBC.eol().insets(0, 8, 0, 0));
                    }
                }
                if (i.addToPanel(itemPanel, itemGuiSupport)) {
                    p.hasElements = true;
                }
            }
            previous = i;
        }
        p.add(itemPanel, GBC.eol().fill());
        p.add(linkPanel, GBC.eol().fill());

        if (selected.isEmpty() && !supportsRelation()) {
            GuiHelper.setEnabledRec(itemPanel, false);
        }

        if (selected.size() == 1 && Boolean.TRUE.equals(USE_VALIDATOR.get())) {
            // Fail early -- validateAsync requires the primitive(s) to be part of a dataset. Failing later in validateAsync ''does not'' give us
            // a usable stack trace. See #21829 for details.
            selected.forEach(OsmPrimitive::checkDataset);
            itemGuiSupport.addListener((source, key, newValue) ->
                    TaggingPresetValidation.validateAsync(selected.iterator().next(), validationLabel, getChangedTags()));
        }

        // "Add toolbar button"
        JToggleButton tb = new JToggleButton(new ToolbarButtonAction());
        tb.setFocusable(false);
        p.add(tb, GBC.std(1, 0).anchor(GBC.LINE_END));

        // Trigger initial updates once and only once
        itemGuiSupport.setEnabled(true);
        itemGuiSupport.fireItemValueModified(null, null, null);

        return p;
    }

    /**
     * Determines whether a dialog can be shown for this preset, i.e., at least one tag can/must be set by the user.
     *
     * @return {@code true} if a dialog can be shown for this preset
     */
    public boolean isShowable() {
        // Not using streams makes this method effectively allocation free and uses ~40% fewer CPU cycles.
        for (TaggingPresetItem i : data) {
            if (!(i instanceof Optional || i instanceof Space || i instanceof Key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Suggests a relation role for this primitive
     *
     * @param osm The primitive
     * @return the suggested role or null
     */
    public String suggestRoleForOsmPrimitive(OsmPrimitive osm) {
        if (roles != null && osm != null) {
            return roles.roles.stream()
                    .filter(i -> i.memberExpression != null && i.memberExpression.match(osm))
                    .filter(i -> Utils.isEmpty(i.types) || i.types.contains(TaggingPresetType.forPrimitive(osm)))
                    .findFirst()
                    .map(i -> i.key)
                    .orElse(null);
        }
        return null;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        DataSet ds = OsmDataManager.getInstance().getEditDataSet();
        if (ds == null) {
            return;
        }
        showAndApply(ds.getSelected());
    }

    /**
     * {@linkplain #showDialog Show preset dialog}, apply changes
     * @param primitives the primitives
     */
    public void showAndApply(Collection<OsmPrimitive> primitives) {
        // Display dialog even if no data layer (used by preset-tagging-tester plugin)
        Collection<OsmPrimitive> sel = createSelection(primitives);
        int answer = showDialog(sel, supportsRelation());

        if (!sel.isEmpty() && answer == DIALOG_ANSWER_APPLY) {
            Command cmd = createCommand(sel, getChangedTags());
            if (cmd != null) {
                UndoRedoHandler.getInstance().add(cmd);
            }
        } else if (answer == DIALOG_ANSWER_NEW_RELATION) {
            Relation calculated = null;
            if (getChangedTags().stream().anyMatch(t -> "boundary".equals(t.get("type")) || "multipolygon".equals(t.get("type")))) {
                Collection<Way> ways = Utils.filteredCollection(primitives, Way.class);
                Pair<Relation, Relation> res = CreateMultipolygonAction.createMultipolygonRelation(ways, true);
                if (res != null) {
                    calculated = res.b;
                }
            }
            final Relation r = calculated != null ? calculated : new Relation();
            final Collection<RelationMember> members = new LinkedHashSet<>(r.getMembers());
            for (Tag t : getChangedTags()) {
                r.put(t.getKey(), t.getValue());
            }
            for (OsmPrimitive osm : primitives) {
                if (r == calculated && osm instanceof Way)
                    continue;
                String role = suggestRoleForOsmPrimitive(osm);
                RelationMember rm = new RelationMember(role == null ? "" : role, osm);
                r.addMember(rm);
                members.add(rm);
            }
            if (r.isMultipolygon() && r != calculated) {
                r.setMembers(RelationSorter.sortMembersByConnectivity(r.getMembers()));
            }
            SwingUtilities.invokeLater(() -> RelationEditor.getEditor(
                    MainApplication.getLayerManager().getEditLayer(), r, members).setVisible(true));
        }
        if (!primitives.isEmpty()) {
            DataSet ds = primitives.iterator().next().getDataSet();
            ds.setSelected(primitives); // force update
        }
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
            super(MainApplication.getMainFrame(), title,
                    showNewRelation ?
                            (new String[] {tr("Apply Preset"), tr("New relation"), tr("Cancel")}) :
                            (new String[] {tr("Apply Preset"), tr("Cancel")}),
                    true);
            if (icon != null)
                setIconImage(icon.getImage());
            contentInsets = new Insets(10, 5, 0, 5);
            if (showNewRelation) {
                setButtonIcons("ok", "data/relation", "cancel");
            } else {
                setButtonIcons("ok", "cancel");
            }
            configureContextsensitiveHelp("/Menu/Presets", true);
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
            int size = sel.size();
            String title = trn("Change {0} object", "Change {0} objects", size, size);
            if (!showNewRelation && size == 0) {
                if (originalSelectionEmpty) {
                    title = tr("Nothing selected!");
                } else {
                    title = tr("Selection unsuitable!");
                }
            }

            boolean disableApply = size == 0;
            if (!disableApply) {
                OsmData<?, ?, ?, ?> ds = sel.iterator().next().getDataSet();
                disableApply = ds != null && ds.isLocked();
            }
            answer = new PresetDialog(p, title, preset_name_label ? null : (ImageIcon) getValue(Action.SMALL_ICON),
                    disableApply, showNewRelation).getValue();
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
        return participants.stream().filter(this::typeMatches).collect(Collectors.toList());
    }

    /**
     * Gets a list of tags that are set by this preset.
     * @return The list of tags.
     */
    public List<Tag> getChangedTags() {
        List<Tag> result = new ArrayList<>();
        data.forEach(i -> i.addCommands(result));
        return result;
    }

    /**
     * Create a command to change the given list of tags.
     * @param sel The primitives to change the tags for
     * @param changedTags The tags to change
     * @return A command that changes the tags.
     */
    public static Command createCommand(Collection<OsmPrimitive> sel, List<Tag> changedTags) {
        List<Command> cmds = changedTags.stream()
                .map(tag -> new ChangePropertyCommand(sel, tag.getKey(), tag.getValue()))
                .filter(cmd -> cmd.getObjectsNumber() > 0)
                .collect(StreamUtils.toUnmodifiableList());
        return cmds.isEmpty() ? null : SequenceCommand.wrapIfNeeded(tr("Change Tags"), cmds);
    }

    private boolean supportsRelation() {
        return types == null || types.contains(TaggingPresetType.RELATION);
    }

    protected final void updateEnabledState() {
        setEnabled(OsmDataManager.getInstance().getEditDataSet() != null);
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
     * Determines whether this preset matches the OSM primitive type.
     * @param primitive The OSM primitive for which type must match
     * @return <code>true</code> if type matches.
     * @since 15640
     */
    public final boolean typeMatches(IPrimitive primitive) {
        return typeMatches(EnumSet.of(TaggingPresetType.forPrimitive(primitive)));
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
     * @since 13623 (signature)
     */
    @Override
    public boolean test(IPrimitive p) {
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
        } else if (matchExpression != null && !matchExpression.match(Tagged.ofMap(tags))) {
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
            super("");
            new ImageProvider("dialogs", "pin").getResource().attachImageIcon(this, true);
            putValue(SHORT_DESCRIPTION, tr("Add or remove toolbar button"));
            List<String> t = new ArrayList<>(ToolbarPreferences.getToolString());
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

    /**
     * Returns the completable future task that performs icon loading, if any.
     * @return the completable future task that performs icon loading, or null
     * @since 14449
     */
    public CompletableFuture<Void> getIconLoadingTask() {
        return iconFuture;
    }

}
