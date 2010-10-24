// License: GPL. Copyright 2007 by Immanuel Scholz and others
// Author: David Earl
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.data.osm.PrimitiveDeepCopy;
import org.openstreetmap.josm.data.osm.PrimitiveDeepCopy.PasteBufferChangedListener;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.TagCollection;
import org.openstreetmap.josm.gui.conflict.tags.PasteTagsConflictResolverDialog;
import org.openstreetmap.josm.tools.Shortcut;

public final class PasteTagsAction extends JosmAction implements PasteBufferChangedListener {

    public PasteTagsAction() {
        super(tr("Paste Tags"), "pastetags",
                tr("Apply tags of contents of paste buffer to all selected items."),
                Shortcut.registerShortcut("system:pastestyle", tr("Edit: {0}", tr("Paste Tags")), KeyEvent.VK_V, Shortcut.GROUP_MENU, Shortcut.SHIFT_DEFAULT), true);
        Main.pasteBuffer.addPasteBufferChangedListener(this);
        putValue("help", ht("/Action/PasteTags"));
    }

    public static class TagPaster {

        private final Collection<PrimitiveData> source;
        private final Collection<OsmPrimitive> target;
        private final List<Tag> commands = new ArrayList<Tag>();

        public TagPaster(Collection<PrimitiveData> source, Collection<OsmPrimitive> target) {
            this.source = source;
            this.target = target;
        }

        /**
         * Replies true if the source for tag pasting is heterogeneous, i.e. if it doesn't consist of
         * {@see OsmPrimitive}s of exactly one type
         *
         * @return
         */
        protected boolean isHeteogeneousSource() {
            int count = 0;
            count = !getSourcePrimitivesByType(OsmPrimitiveType.NODE).isEmpty() ? count + 1 : count;
            count = !getSourcePrimitivesByType(OsmPrimitiveType.WAY).isEmpty() ? count + 1 : count;
            count = !getSourcePrimitivesByType(OsmPrimitiveType.RELATION).isEmpty() ? count + 1 : count;
            return count > 1;
        }

        /**
         * Replies all primitives of type <code>type</code> in the current selection.
         *
         * @param <T>
         * @param type  the type
         * @return all primitives of type <code>type</code> in the current selection.
         */
        protected <T extends PrimitiveData> Collection<? extends PrimitiveData> getSourcePrimitivesByType(OsmPrimitiveType type) {
            return PrimitiveData.getFilteredList(source, type);
        }

        /**
         * Replies the collection of tags for all primitives of type <code>type</code> in the current
         * selection
         *
         * @param <T>
         * @param type  the type
         * @return the collection of tags for all primitives of type <code>type</code> in the current
         * selection
         */
        protected <T extends OsmPrimitive> TagCollection getSourceTagsByType(OsmPrimitiveType type) {
            return TagCollection.unionOfAllPrimitives(getSourcePrimitivesByType(type));
        }

        /**
         * Replies true if there is at least one tag in the current selection for primitives of
         * type <code>type</code>
         *
         * @param <T>
         * @param type the type
         * @return true if there is at least one tag in the current selection for primitives of
         * type <code>type</code>
         */
        protected <T extends OsmPrimitive> boolean hasSourceTagsByType(OsmPrimitiveType type) {
            return ! getSourceTagsByType(type).isEmpty();
        }

        protected void buildChangeCommand(Collection<? extends OsmPrimitive> selection, TagCollection tc) {
            for (String key : tc.getKeys()) {
                commands.add(new Tag(key, tc.getValues(key).iterator().next()));
            }
        }

        protected Map<OsmPrimitiveType, Integer> getSourceStatistics() {
            HashMap<OsmPrimitiveType, Integer> ret = new HashMap<OsmPrimitiveType, Integer>();
            for (OsmPrimitiveType type: OsmPrimitiveType.values()) {
                if (!getSourceTagsByType(type).isEmpty()) {
                    ret.put(type, getSourcePrimitivesByType(type).size());
                }
            }
            return ret;
        }

        protected Map<OsmPrimitiveType, Integer> getTargetStatistics() {
            HashMap<OsmPrimitiveType, Integer> ret = new HashMap<OsmPrimitiveType, Integer>();
            for (OsmPrimitiveType type: OsmPrimitiveType.values()) {
                int count = OsmPrimitive.getFilteredList(target, type.getOsmClass()).size();
                if (count > 0) {
                    ret.put(type, count);
                }
            }
            return ret;
        }

        /**
         * Pastes the tags from a homogeneous source (i.e. the {@see Main#pasteBuffer}s selection consisting
         * of one type of {@see OsmPrimitive}s only.
         *
         * Tags from a homogeneous source can be pasted to a heterogeneous target. All target primitives,
         * regardless of their type, receive the same tags.
         *
         * @param targets the collection of target primitives
         */
        protected void pasteFromHomogeneousSource() {
            TagCollection tc = null;
            for (OsmPrimitiveType type : OsmPrimitiveType.values()) {
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
                buildChangeCommand(target, dialog.getResolution());
            } else {
                // no conflicts in the source tags to resolve. Just apply the tags
                // to the target primitives
                //
                buildChangeCommand(target, tc);
            }
        }

        /**
         * Replies true if there is at least one primitive of type <code>type</code> in the collection
         * <code>selection</code>
         *
         * @param <T>
         * @param selection  the collection of primitives
         * @param type  the type to look for
         * @return true if there is at least one primitive of type <code>type</code> in the collection
         * <code>selection</code>
         */
        protected <T extends OsmPrimitive> boolean hasTargetPrimitives(Class<T> type) {
            return !OsmPrimitive.getFilteredList(target, type).isEmpty();
        }

        /**
         * Replies true if this a heterogeneous source can be pasted without conflict to targets
         *
         * @param targets the collection of target primitives
         * @return true if this a heterogeneous source can be pasted without conflicts to targets
         */
        protected boolean canPasteFromHeterogeneousSourceWithoutConflict(Collection<OsmPrimitive> targets) {
            for (OsmPrimitiveType type:OsmPrimitiveType.values()) {
                if (hasTargetPrimitives(type.getOsmClass())) {
                    TagCollection tc = TagCollection.unionOfAllPrimitives(getSourcePrimitivesByType(type));
                    if (!tc.isEmpty() && ! tc.isApplicableToPrimitive())
                        return false;
                }
            }
            return true;
        }

        /**
         * Pastes the tags in the current selection of the paste buffer to a set of target
         * primitives.
         *
         * @param targets the collection of target primitives
         */
        protected void pasteFromHeterogeneousSource() {
            if (canPasteFromHeterogeneousSourceWithoutConflict(target)) {
                for (OsmPrimitiveType type:OsmPrimitiveType.values()) {
                    if (hasSourceTagsByType(type) && hasTargetPrimitives(type.getOsmClass())) {
                        buildChangeCommand(target, getSourceTagsByType(type));
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
                for (OsmPrimitiveType type:OsmPrimitiveType.values()) {
                    if (hasSourceTagsByType(type) && hasTargetPrimitives(type.getOsmClass())) {
                        buildChangeCommand(OsmPrimitive.getFilteredList(target, type.getOsmClass()), dialog.getResolution(type));
                    }
                }
            }
        }

        public List<Tag> execute() {
            commands.clear();
            if (isHeteogeneousSource()) {
                pasteFromHeterogeneousSource();
            } else {
                pasteFromHomogeneousSource();
            }
            return commands;
        }

    }

    public void actionPerformed(ActionEvent e) {
        Collection<OsmPrimitive> selection = getCurrentDataSet().getSelected();

        if (selection.isEmpty())
            return;

        TagPaster tagPaster = new TagPaster(Main.pasteBuffer.getDirectlyAdded(), selection);

        List<Command> commands = new ArrayList<Command>();
        for (Tag tag: tagPaster.execute()) {
            commands.add(new ChangePropertyCommand(selection, tag.getKey(), "".equals(tag.getValue())?null:tag.getValue()));
        }
        if (!commands.isEmpty()) {
            String title1 = trn("Pasting {0} tag", "Pasting {0} tags", commands.size(), commands.size());
            String title2 = trn("to {0} primitive", "to {0} primtives", selection.size(), selection.size());
            Main.main.undoRedo.add(
                    new SequenceCommand(
                            title1 + " " + title2,
                            commands
                    ));
        }

    }

    @Override public void pasteBufferChanged(PrimitiveDeepCopy newPasteBuffer) {
        updateEnabledState();
    }

    @Override
    protected void updateEnabledState() {
        if (getCurrentDataSet() == null || Main.pasteBuffer == null) {
            setEnabled(false);
            return;
        }
        setEnabled(
                !getCurrentDataSet().getSelected().isEmpty()
                && !TagCollection.unionOfAllPrimitives(Main.pasteBuffer.getDirectlyAdded()).isEmpty()
        );
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        setEnabled(
                selection!= null && !selection.isEmpty()
                && !TagCollection.unionOfAllPrimitives(Main.pasteBuffer.getDirectlyAdded()).isEmpty()
        );
    }
}
