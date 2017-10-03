// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.datatransfer.importers;

import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.TransferHandler.TransferSupport;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.TagCollection;
import org.openstreetmap.josm.data.osm.TagMap;
import org.openstreetmap.josm.gui.conflict.tags.PasteTagsConflictResolverDialog;
import org.openstreetmap.josm.gui.datatransfer.data.PrimitiveTagTransferData;

/**
 * This class helps pasting tags from other primitives. It handles resolving conflicts.
 * @author Michael Zangl
 * @since 10737
 */
public class PrimitiveTagTransferPaster extends AbstractTagPaster {
    /**
     * Create a new {@link PrimitiveTagTransferPaster}
     */
    public PrimitiveTagTransferPaster() {
        super(PrimitiveTagTransferData.FLAVOR);
    }

    @Override
    public boolean importTagsOn(TransferSupport support, Collection<? extends OsmPrimitive> selection)
            throws UnsupportedFlavorException, IOException {
        Object o = support.getTransferable().getTransferData(df);
        if (!(o instanceof PrimitiveTagTransferData))
            return false;
        PrimitiveTagTransferData data = (PrimitiveTagTransferData) o;

        TagPasteSupport tagPaster = new TagPasteSupport(data, selection);
        List<Command> commands = new ArrayList<>();
        for (Tag tag : tagPaster.execute()) {
            Map<String, String> tags = new HashMap<>(1);
            tags.put(tag.getKey(), "".equals(tag.getValue()) ? null : tag.getValue());
            ChangePropertyCommand cmd = new ChangePropertyCommand(Main.main.getEditDataSet(), selection, tags);
            if (cmd.getObjectsNumber() > 0) {
                commands.add(cmd);
            }
        }
        commitCommands(selection, commands);
        return true;
    }

    @Override
    protected Map<String, String> getTags(TransferSupport support) throws UnsupportedFlavorException, IOException {
        PrimitiveTagTransferData data = (PrimitiveTagTransferData) support.getTransferable().getTransferData(df);

        TagPasteSupport tagPaster = new TagPasteSupport(data, Arrays.asList(new Node()));
        return new TagMap(tagPaster.execute());
    }

    private static class TagPasteSupport {
        private final PrimitiveTagTransferData data;
        private final Collection<? extends IPrimitive> selection;
        private final List<Tag> tags = new ArrayList<>();

        /**
         * Constructs a new {@code TagPasteSupport}.
         * @param data source tags to paste
         * @param selection target primitives
         */
        TagPasteSupport(PrimitiveTagTransferData data, Collection<? extends IPrimitive> selection) {
            super();
            this.data = data;
            this.selection = selection;
        }

        /**
         * Pastes the tags from a homogeneous source (the selection consisting
         * of one type of {@link OsmPrimitive}s only).
         *
         * Tags from a homogeneous source can be pasted to a heterogeneous target. All target primitives,
         * regardless of their type, receive the same tags.
         */
        protected void pasteFromHomogeneousSource() {
            TagCollection tc = null;
            for (OsmPrimitiveType type : OsmPrimitiveType.dataValues()) {
                TagCollection tc1 = data.getForPrimitives(type);
                if (!tc1.isEmpty()) {
                    tc = tc1;
                }
            }
            if (tc == null)
                // no tags found to paste. Abort.
                return;

            if (!tc.isApplicableToPrimitive()) {
                PasteTagsConflictResolverDialog dialog = new PasteTagsConflictResolverDialog(Main.parent);
                dialog.populate(tc, data.getStatistics(), getTargetStatistics());
                dialog.setVisible(true);
                if (dialog.isCanceled())
                    return;
                buildTags(dialog.getResolution());
            } else {
                // no conflicts in the source tags to resolve. Just apply the tags to the target primitives
                buildTags(tc);
            }
        }

        /**
         * Replies true if this a heterogeneous source can be pasted without conflict to targets
         *
         * @return true if this a heterogeneous source can be pasted without conflicts to targets
         */
        protected boolean canPasteFromHeterogeneousSourceWithoutConflict() {
            for (OsmPrimitiveType type : OsmPrimitiveType.dataValues()) {
                if (hasTargetPrimitives(type)) {
                    TagCollection tc = data.getForPrimitives(type);
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
                    if (!data.getForPrimitives(type).isEmpty() && hasTargetPrimitives(type)) {
                        buildTags(data.getForPrimitives(type));
                    }
                }
            } else {
                PasteTagsConflictResolverDialog dialog = new PasteTagsConflictResolverDialog(Main.parent);
                dialog.populate(
                        data.getForPrimitives(OsmPrimitiveType.NODE),
                        data.getForPrimitives(OsmPrimitiveType.WAY),
                        data.getForPrimitives(OsmPrimitiveType.RELATION),
                        data.getStatistics(),
                        getTargetStatistics()
                );
                dialog.setVisible(true);
                if (dialog.isCanceled())
                    return;
                for (OsmPrimitiveType type : OsmPrimitiveType.dataValues()) {
                    if (!data.getForPrimitives(type).isEmpty() && hasTargetPrimitives(type)) {
                        buildTags(dialog.getResolution(type));
                    }
                }
            }
        }

        protected Map<OsmPrimitiveType, Integer> getTargetStatistics() {
            Map<OsmPrimitiveType, Integer> ret = new EnumMap<>(OsmPrimitiveType.class);
            for (OsmPrimitiveType type: OsmPrimitiveType.dataValues()) {
                int count = (int) selection.stream().filter(p -> type == p.getType()).count();
                if (count > 0) {
                    ret.put(type, count);
                }
            }
            return ret;
        }

        /**
         * Replies true if there is at least one primitive of type <code>type</code>
         * is in the target collection
         *
         * @param type  the type to look for
         * @return true if there is at least one primitive of type <code>type</code> in the collection
         * <code>selection</code>
         */
        protected boolean hasTargetPrimitives(OsmPrimitiveType type) {
            return selection.stream().anyMatch(p -> type == p.getType());
        }

        protected void buildTags(TagCollection tc) {
            for (String key : tc.getKeys()) {
                tags.add(new Tag(key, tc.getValues(key).iterator().next()));
            }
        }

        /**
         * Performs the paste operation.
         * @return list of tags
         */
        public List<Tag> execute() {
            tags.clear();
            if (data.isHeterogeneousSource()) {
                pasteFromHeterogeneousSource();
            } else {
                pasteFromHomogeneousSource();
            }
            return tags;
        }

        @Override
        public String toString() {
            return "PasteSupport [data=" + data + ", selection=" + selection + ']';
        }
    }
}
