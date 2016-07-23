// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.TagCollection;
import org.openstreetmap.josm.gui.conflict.tags.PasteTagsConflictResolverDialog;
import org.openstreetmap.josm.gui.datatransfer.OsmTransferHandler;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.TextTagParser;

/**
 * Action, to paste all tags from one primitive to another.
 *
 * It will take the primitive from the copy-paste buffer an apply all its tags
 * to the selected primitive(s).
 *
 * @author David Earl
 */
public final class PasteTagsAction extends JosmAction {

    private static final String help = ht("/Action/PasteTags");
    private final OsmTransferHandler transferHandler = new OsmTransferHandler();

    /**
     * Constructs a new {@code PasteTagsAction}.
     */
    public PasteTagsAction() {
        super(tr("Paste Tags"), "pastetags",
                tr("Apply tags of contents of paste buffer to all selected items."),
                Shortcut.registerShortcut("system:pastestyle", tr("Edit: {0}", tr("Paste Tags")),
                KeyEvent.VK_V, Shortcut.CTRL_SHIFT), true);
        putValue("help", help);
    }

    /**
     * Used to update the tags.
     */
    public static class TagPaster {

        private final Collection<PrimitiveData> source;
        private final Collection<OsmPrimitive> target;
        private final List<Tag> tags = new ArrayList<>();

        /**
         * Constructs a new {@code TagPaster}.
         * @param source source primitives
         * @param target target primitives
         */
        public TagPaster(Collection<PrimitiveData> source, Collection<OsmPrimitive> target) {
            this.source = source;
            this.target = target;
        }

        /**
         * Determines if the source for tag pasting is heterogeneous, i.e. if it doesn't consist of
         * {@link OsmPrimitive}s of exactly one type
         * @return true if the source for tag pasting is heterogeneous
         */
        protected boolean isHeterogeneousSource() {
            int count = 0;
            count = !getSourcePrimitivesByType(OsmPrimitiveType.NODE).isEmpty() ? (count + 1) : count;
            count = !getSourcePrimitivesByType(OsmPrimitiveType.WAY).isEmpty() ? (count + 1) : count;
            count = !getSourcePrimitivesByType(OsmPrimitiveType.RELATION).isEmpty() ? (count + 1) : count;
            return count > 1;
        }

        /**
         * Replies all primitives of type <code>type</code> in the current selection.
         *
         * @param type  the type
         * @return all primitives of type <code>type</code> in the current selection.
         */
        protected Collection<? extends PrimitiveData> getSourcePrimitivesByType(OsmPrimitiveType type) {
            return PrimitiveData.getFilteredList(source, type);
        }

        /**
         * Replies the collection of tags for all primitives of type <code>type</code> in the current
         * selection
         *
         * @param type  the type
         * @return the collection of tags for all primitives of type <code>type</code> in the current
         * selection
         */
        protected TagCollection getSourceTagsByType(OsmPrimitiveType type) {
            return TagCollection.unionOfAllPrimitives(getSourcePrimitivesByType(type));
        }

        /**
         * Replies true if there is at least one tag in the current selection for primitives of
         * type <code>type</code>
         *
         * @param type the type
         * @return true if there is at least one tag in the current selection for primitives of
         * type <code>type</code>
         */
        protected boolean hasSourceTagsByType(OsmPrimitiveType type) {
            return !getSourceTagsByType(type).isEmpty();
        }

        protected void buildTags(TagCollection tc) {
            for (String key : tc.getKeys()) {
                tags.add(new Tag(key, tc.getValues(key).iterator().next()));
            }
        }

        protected Map<OsmPrimitiveType, Integer> getSourceStatistics() {
            Map<OsmPrimitiveType, Integer> ret = new EnumMap<>(OsmPrimitiveType.class);
            for (OsmPrimitiveType type: OsmPrimitiveType.dataValues()) {
                if (!getSourceTagsByType(type).isEmpty()) {
                    ret.put(type, getSourcePrimitivesByType(type).size());
                }
            }
            return ret;
        }

        protected Map<OsmPrimitiveType, Integer> getTargetStatistics() {
            Map<OsmPrimitiveType, Integer> ret = new EnumMap<>(OsmPrimitiveType.class);
            for (OsmPrimitiveType type: OsmPrimitiveType.dataValues()) {
                int count = OsmPrimitive.getFilteredList(target, type.getOsmClass()).size();
                if (count > 0) {
                    ret.put(type, count);
                }
            }
            return ret;
        }

        /**
         * Pastes the tags from a homogeneous source (the {@link Main#pasteBuffer}s selection consisting
         * of one type of {@link OsmPrimitive}s only).
         *
         * Tags from a homogeneous source can be pasted to a heterogeneous target. All target primitives,
         * regardless of their type, receive the same tags.
         */
        protected void pasteFromHomogeneousSource() {
            TagCollection tc = null;
            for (OsmPrimitiveType type : OsmPrimitiveType.dataValues()) {
                TagCollection tc1 = getSourceTagsByType(type);
                if (!tc1.isEmpty()) {
                    tc = tc1;
                }
            }
            if (tc == null)
                // no tags found to paste. Abort.
                return;

            if (!tc.isApplicableToPrimitive()) {
                PasteTagsConflictResolverDialog dialog = new PasteTagsConflictResolverDialog(Main.parent);
                dialog.populate(tc, getSourceStatistics(), getTargetStatistics());
                dialog.setVisible(true);
                if (dialog.isCanceled())
                    return;
                buildTags(dialog.getResolution());
            } else {
                // no conflicts in the source tags to resolve. Just apply the tags
                // to the target primitives
                //
                buildTags(tc);
            }
        }

        /**
         * Replies true if there is at least one primitive of type <code>type</code>
         * is in the target collection
         *
         * @param type  the type to look for
         * @return true if there is at least one primitive of type <code>type</code> in the collection
         * <code>selection</code>
         */
        protected boolean hasTargetPrimitives(Class<? extends OsmPrimitive> type) {
            return !OsmPrimitive.getFilteredList(target, type).isEmpty();
        }

        /**
         * Replies true if this a heterogeneous source can be pasted without conflict to targets
         *
         * @return true if this a heterogeneous source can be pasted without conflicts to targets
         */
        protected boolean canPasteFromHeterogeneousSourceWithoutConflict() {
            for (OsmPrimitiveType type : OsmPrimitiveType.dataValues()) {
                if (hasTargetPrimitives(type.getOsmClass())) {
                    TagCollection tc = TagCollection.unionOfAllPrimitives(getSourcePrimitivesByType(type));
                    if (!tc.isEmpty() && !tc.isApplicableToPrimitive())
                        return false;
                }
            }
            return true;
        }

        /**
         * Pastes the tags in the current selection of the paste buffer to a set of target primitives.
         */
        protected void pasteFromHeterogeneousSource() {
            if (canPasteFromHeterogeneousSourceWithoutConflict()) {
                for (OsmPrimitiveType type : OsmPrimitiveType.dataValues()) {
                    if (hasSourceTagsByType(type) && hasTargetPrimitives(type.getOsmClass())) {
                        buildTags(getSourceTagsByType(type));
                    }
                }
            } else {
                PasteTagsConflictResolverDialog dialog = new PasteTagsConflictResolverDialog(Main.parent);
                dialog.populate(
                        getSourceTagsByType(OsmPrimitiveType.NODE),
                        getSourceTagsByType(OsmPrimitiveType.WAY),
                        getSourceTagsByType(OsmPrimitiveType.RELATION),
                        getSourceStatistics(),
                        getTargetStatistics()
                );
                dialog.setVisible(true);
                if (dialog.isCanceled())
                    return;
                for (OsmPrimitiveType type : OsmPrimitiveType.dataValues()) {
                    if (hasSourceTagsByType(type) && hasTargetPrimitives(type.getOsmClass())) {
                        buildTags(dialog.getResolution(type));
                    }
                }
            }
        }

        /**
         * Performs the paste operation.
         * @return list of tags
         */
        public List<Tag> execute() {
            tags.clear();
            if (isHeterogeneousSource()) {
                pasteFromHeterogeneousSource();
            } else {
                pasteFromHomogeneousSource();
            }
            return tags;
        }

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Collection<OsmPrimitive> selection = getLayerManager().getEditDataSet().getSelected();

        if (selection.isEmpty())
            return;

        transferHandler.pasteTags(selection);
    }

    /**
     * Paste tags from arbitrary text, not using JOSM buffer
     * @param selection selected primitives
     * @param text text containing tags
     * @return true if action was successful
     * @see TextTagParser#readTagsFromText
     */
    public static boolean pasteTagsFromText(Collection<OsmPrimitive> selection, String text) {
        Map<String, String> tags = TextTagParser.readTagsFromText(text);
        if (tags == null || tags.isEmpty()) {
            TextTagParser.showBadBufferMessage(help);
            return false;
        }
        if (!TextTagParser.validateTags(tags)) return false;

        List<Command> commands = new ArrayList<>(tags.size());
        for (Entry<String, String> entry: tags.entrySet()) {
            String v = entry.getValue();
            commands.add(new ChangePropertyCommand(selection, entry.getKey(), "".equals(v) ? null : v));
        }
        commitCommands(selection, commands);
        return !commands.isEmpty();
    }

    /**
     * Create and execute SequenceCommand with descriptive title
     * @param selection selected primitives
     * @param commands the commands to perform in a sequential command
     */
    private static void commitCommands(Collection<OsmPrimitive> selection, List<Command> commands) {
        if (!commands.isEmpty()) {
            String title1 = trn("Pasting {0} tag", "Pasting {0} tags", commands.size(), commands.size());
            String title2 = trn("to {0} object", "to {0} objects", selection.size(), selection.size());
            @I18n.QuirkyPluralString
            final String title = title1 + ' ' + title2;
            Main.main.undoRedo.add(
                    new SequenceCommand(
                            title,
                            commands
                    ));
        }
    }

    @Override
    protected void updateEnabledState() {
        DataSet ds = getLayerManager().getEditDataSet();
        if (ds == null) {
            setEnabled(false);
            return;
        }
        // buffer listening slows down the program and is not very good for arbitrary text in buffer
        setEnabled(!ds.selectionEmpty());
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        setEnabled(selection != null && !selection.isEmpty());
    }
}
