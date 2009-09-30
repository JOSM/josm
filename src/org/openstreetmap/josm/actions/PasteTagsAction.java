// License: GPL. Copyright 2007 by Immanuel Scholz and others
// Author: David Earl
package org.openstreetmap.josm.actions;

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
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.TagCollection;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.conflict.tags.PasteTagsConflictResolverDialog;
import org.openstreetmap.josm.tools.Shortcut;

public final class PasteTagsAction extends JosmAction {

    public PasteTagsAction(JosmAction copyAction) {
        super(tr("Paste Tags"), "pastetags",
                tr("Apply tags of contents of paste buffer to all selected items."),
                Shortcut.registerShortcut("system:pastestyle", tr("Edit: {0}", tr("Paste Tags")), KeyEvent.VK_V, Shortcut.GROUP_MENU, Shortcut.SHIFT_DEFAULT), true);
        copyAction.addListener(this);
    }

    static private List<Class<? extends OsmPrimitive>> osmPrimitiveClasses;
    static {
        osmPrimitiveClasses = new ArrayList<Class<? extends OsmPrimitive>>();
        osmPrimitiveClasses.add(Node.class);
        osmPrimitiveClasses.add(Way.class);
        osmPrimitiveClasses.add(Relation.class);
    }

    /**
     * Replies true if the source for tag pasting is heterogeneous, i.e. if it doesn't consist of
     * {@see OsmPrimitive}s of exactly one type
     * 
     * @return
     */
    protected boolean isHeteogeneousSource() {
        int count = 0;
        count = !getSourcePrimitivesByType(Node.class).isEmpty() ? count + 1 : count;
        count = !getSourcePrimitivesByType(Way.class).isEmpty() ? count + 1 : count;
        count = !getSourcePrimitivesByType(Relation.class).isEmpty() ? count + 1 : count;
        return count > 1;
    }

    /**
     * Replies all primitives of type <code>type</code> in the current selection.
     * 
     * @param <T>
     * @param type  the type
     * @return all primitives of type <code>type</code> in the current selection.
     */
    protected <T extends OsmPrimitive> Collection<? extends OsmPrimitive> getSourcePrimitivesByType(Class<T> type) {
        return OsmPrimitive.getFilteredList(Main.pasteBuffer.getSelected(), type);
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
    protected <T extends OsmPrimitive> TagCollection getSourceTagsByType(Class<T> type) {
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
    protected <T extends OsmPrimitive> boolean hasSourceTagsByType(Class<T> type) {
        return ! getSourceTagsByType(type).isEmpty();
    }

    protected Command buildChangeCommand(Collection<? extends OsmPrimitive> selection, TagCollection tc) {
        List<Command> commands = new ArrayList<Command>();
        for (String key : tc.getKeys()) {
            String value = tc.getValues(key).iterator().next();
            value = value.equals("") ? null : value;
            commands.add(new ChangePropertyCommand(selection,key,value));
        }
        if (!commands.isEmpty()) {
            String title1 = trn("Pasting {0} tag", "Pasting {0} tags", tc.getKeys().size(), tc.getKeys().size());
            String title2 = trn("to {0} primitive", "to {0} primtives", selection.size(), selection.size());
            return new SequenceCommand(
                    title1 + " " + title2,
                    commands
            );
        }
        return null;
    }

    protected Map<OsmPrimitiveType, Integer> getSourceStatistics() {
        HashMap<OsmPrimitiveType, Integer> ret = new HashMap<OsmPrimitiveType, Integer>();
        for (Class<? extends OsmPrimitive> type: osmPrimitiveClasses) {
            if (!getSourceTagsByType(type).isEmpty()) {
                ret.put(OsmPrimitiveType.from(type), getSourcePrimitivesByType(type).size());
            }
        }
        return ret;
    }

    protected Map<OsmPrimitiveType, Integer> getTargetStatistics() {
        HashMap<OsmPrimitiveType, Integer> ret = new HashMap<OsmPrimitiveType, Integer>();
        for (Class<? extends OsmPrimitive> type: osmPrimitiveClasses) {
            int count = OsmPrimitive.getFilteredList(getEditLayer().data.getSelected(), type).size();
            if (count > 0) {
                ret.put(OsmPrimitiveType.from(type), count);
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
    protected void pasteFromHomogeneousSource(Collection<? extends OsmPrimitive> targets) {
        TagCollection tc = null;
        for (Class<? extends OsmPrimitive> type : osmPrimitiveClasses) {
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
            Command cmd = buildChangeCommand(targets, dialog.getResolution());
            Main.main.undoRedo.add(cmd);
        } else {
            // no conflicts in the source tags to resolve. Just apply the tags
            // to the target primitives
            //
            Command cmd = buildChangeCommand(targets, tc);
            Main.main.undoRedo.add(cmd);
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
    protected <T extends OsmPrimitive> boolean hasTargetPrimitives(Collection<OsmPrimitive> selection, Class<T> type) {
        return !OsmPrimitive.getFilteredList(selection, type).isEmpty();
    }

    /**
     * Replies true if this a heterogeneous source can be pasted without conflict to targets
     * 
     * @param targets the collection of target primitives
     * @return true if this a heterogeneous source can be pasted without conflicts to targets
     */
    protected boolean canPasteFromHeterogeneousSourceWithoutConflict(Collection<OsmPrimitive> targets) {
        if (hasTargetPrimitives(targets, Node.class)) {
            TagCollection tc = TagCollection.unionOfAllPrimitives(getSourcePrimitivesByType(Node.class));
            if (!tc.isEmpty() && ! tc.isApplicableToPrimitive())
                return false;
        }
        if (hasTargetPrimitives(targets, Way.class)) {
            TagCollection tc = TagCollection.unionOfAllPrimitives(getSourcePrimitivesByType(Way.class));
            if (!tc.isEmpty() && ! tc.isApplicableToPrimitive())
                return false;
        }
        if (hasTargetPrimitives(targets, Relation.class)) {
            TagCollection tc = TagCollection.unionOfAllPrimitives(getSourcePrimitivesByType(Relation.class));
            if (!tc.isEmpty() && ! tc.isApplicableToPrimitive())
                return false;
        }
        return true;
    }

    /**
     * Pastes the tags in the current selection of the paste buffer to a set of target
     * primitives.
     * 
     * @param targets the collection of target primitives
     */
    protected void pasteFromHeterogeneousSource(Collection<OsmPrimitive> targets) {
        if (canPasteFromHeterogeneousSourceWithoutConflict(targets)) {
            if (hasSourceTagsByType(Node.class) && hasTargetPrimitives(targets, Node.class)) {
                Command cmd = buildChangeCommand(targets, getSourceTagsByType(Node.class));
                Main.main.undoRedo.add(cmd);
            }
            if (hasSourceTagsByType(Way.class) && hasTargetPrimitives(targets, Way.class)) {
                Command cmd = buildChangeCommand(targets, getSourceTagsByType(Way.class));
                Main.main.undoRedo.add(cmd);
            }
            if (hasSourceTagsByType(Relation.class) && hasTargetPrimitives(targets, Relation.class)) {
                Command cmd = buildChangeCommand(targets,getSourceTagsByType(Relation.class));
                Main.main.undoRedo.add(cmd);
            }
        } else {
            PasteTagsConflictResolverDialog dialog = new PasteTagsConflictResolverDialog(Main.parent);
            dialog.populate(
                    getSourceTagsByType(Node.class),
                    getSourceTagsByType(Way.class),
                    getSourceTagsByType(Relation.class),
                    getSourceStatistics(),
                    getTargetStatistics()
            );
            dialog.setVisible(true);
            if (dialog.isCanceled())
                return;
            if (hasSourceTagsByType(Node.class) && hasTargetPrimitives(targets, Node.class)) {
                Command cmd = buildChangeCommand(OsmPrimitive.getFilteredList(targets, Node.class), dialog.getResolution(OsmPrimitiveType.NODE));
                Main.main.undoRedo.add(cmd);
            }
            if (hasSourceTagsByType(Way.class) && hasTargetPrimitives(targets, Way.class)) {
                Command cmd = buildChangeCommand(OsmPrimitive.getFilteredList(targets, Way.class), dialog.getResolution(OsmPrimitiveType.WAY));
                Main.main.undoRedo.add(cmd);
            }
            if (hasSourceTagsByType(Relation.class) && hasTargetPrimitives(targets, Relation.class)) {
                Command cmd = buildChangeCommand(OsmPrimitive.getFilteredList(targets, Relation.class), dialog.getResolution(OsmPrimitiveType.RELATION));
                Main.main.undoRedo.add(cmd);
            }
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (getCurrentDataSet().getSelected().isEmpty())
            return;
        if (isHeteogeneousSource()) {
            pasteFromHeterogeneousSource(getCurrentDataSet().getSelected());
        } else {
            pasteFromHomogeneousSource(getCurrentDataSet().getSelected());
        }
    }

    @Override public void pasteBufferChanged(DataSet newPasteBuffer) {
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
                && !TagCollection.unionOfAllPrimitives(Main.pasteBuffer.getSelected()).isEmpty()
        );
    }
}
