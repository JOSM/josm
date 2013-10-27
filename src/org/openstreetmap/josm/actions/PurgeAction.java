// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.PurgeCommand;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.OsmPrimitivRenderer;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * The action to purge the selected primitives, i.e. remove them from the
 * data layer, or remove their content and make them incomplete.
 *
 * This means, the deleted flag is not affected and JOSM simply forgets
 * about these primitives.
 *
 * This action is undo-able. In order not to break previous commands in the
 * undo buffer, we must re-add the identical object (and not semantically
 * equal ones).
 */
public class PurgeAction extends JosmAction {

    public PurgeAction() {
        /* translator note: other expressions for "purge" might be "forget", "clean", "obliterate", "prune" */
        super(tr("Purge..."), "purge",  tr("Forget objects but do not delete them on server when uploading."),
                Shortcut.registerShortcut("system:purge", tr("Edit: {0}", tr("Purge")),
                KeyEvent.VK_P, Shortcut.CTRL_SHIFT),
                true);
        putValue("help", HelpUtil.ht("/Action/Purge"));
    }

    protected OsmDataLayer layer;
    JCheckBox cbClearUndoRedo;

    protected Set<OsmPrimitive> toPurge;
    /**
     * finally, contains all objects that are purged
     */
    protected Set<OsmPrimitive> toPurgeChecked;
    /**
     * Subset of toPurgeChecked. Marks primitives that remain in the
     * dataset, but incomplete.
     */
    protected Set<OsmPrimitive> makeIncomplete;
    /**
     * Subset of toPurgeChecked. Those that have not been in the selection.
     */
    protected List<OsmPrimitive> toPurgeAdditionally;

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;

        Collection<OsmPrimitive> sel = getCurrentDataSet().getAllSelected();
        layer = Main.main.getEditLayer();

        toPurge = new HashSet<OsmPrimitive>(sel);
        toPurgeAdditionally = new ArrayList<OsmPrimitive>();
        toPurgeChecked = new HashSet<OsmPrimitive>();

        // Add referrer, unless the object to purge is not new
        // and the parent is a relation
        HashSet<OsmPrimitive> toPurgeRecursive = new HashSet<OsmPrimitive>();
        while (!toPurge.isEmpty()) {

            for (OsmPrimitive osm: toPurge) {
                for (OsmPrimitive parent: osm.getReferrers()) {
                    if (toPurge.contains(parent) || toPurgeChecked.contains(parent) || toPurgeRecursive.contains(parent)) {
                        continue;
                    }
                    if (parent instanceof Way || (parent instanceof Relation && osm.isNew())) {
                        toPurgeAdditionally.add(parent);
                        toPurgeRecursive.add(parent);
                    }
                }
                toPurgeChecked.add(osm);
            }
            toPurge = toPurgeRecursive;
            toPurgeRecursive = new HashSet<OsmPrimitive>();
        }

        makeIncomplete = new HashSet<OsmPrimitive>();

        // Find the objects that will be incomplete after purging.
        // At this point, all parents of new to-be-purged primitives are
        // also to-be-purged and
        // all parents of not-new to-be-purged primitives are either
        // to-be-purged or of type relation.
        TOP:
            for (OsmPrimitive child : toPurgeChecked) {
                if (child.isNew()) {
                    continue;
                }
                for (OsmPrimitive parent : child.getReferrers()) {
                    if (parent instanceof Relation && !toPurgeChecked.contains(parent)) {
                        makeIncomplete.add(child);
                        continue TOP;
                    }
                }
            }

        // Add untagged way nodes. Do not add nodes that have other
        // referrers not yet to-be-purged.
        if (Main.pref.getBoolean("purge.add_untagged_waynodes", true)) {
            Set<OsmPrimitive> wayNodes = new HashSet<OsmPrimitive>();
            for (OsmPrimitive osm : toPurgeChecked) {
                if (osm instanceof Way) {
                    Way w = (Way) osm;
                    NODE:
                        for (Node n : w.getNodes()) {
                            if (n.isTagged() || toPurgeChecked.contains(n)) {
                                continue;
                            }
                            for (OsmPrimitive ref : n.getReferrers()) {
                                if (ref != w && !toPurgeChecked.contains(ref)) {
                                    continue NODE;
                                }
                            }
                            wayNodes.add(n);
                        }
                }
            }
            toPurgeChecked.addAll(wayNodes);
            toPurgeAdditionally.addAll(wayNodes);
        }

        if (Main.pref.getBoolean("purge.add_relations_with_only_incomplete_members", true)) {
            Set<Relation> relSet = new HashSet<Relation>();
            for (OsmPrimitive osm : toPurgeChecked) {
                for (OsmPrimitive parent : osm.getReferrers()) {
                    if (parent instanceof Relation
                            && !(toPurgeChecked.contains(parent))
                            && hasOnlyIncompleteMembers((Relation) parent, toPurgeChecked, relSet)) {
                        relSet.add((Relation) parent);
                    }
                }
            }

            /**
             * Add higher level relations (list gets extended while looping over it)
             */
            List<Relation> relLst = new ArrayList<Relation>(relSet);
            for (int i=0; i<relLst.size(); ++i) {
                for (OsmPrimitive parent : relLst.get(i).getReferrers()) {
                    if (!(toPurgeChecked.contains(parent))
                            && hasOnlyIncompleteMembers((Relation) parent, toPurgeChecked, relLst)) {
                        relLst.add((Relation) parent);
                    }
                }
            }
            relSet = new HashSet<Relation>(relLst);
            toPurgeChecked.addAll(relSet);
            toPurgeAdditionally.addAll(relSet);
        }

        boolean modified = false;
        for (OsmPrimitive osm : toPurgeChecked) {
            if (osm.isModified()) {
                modified = true;
                break;
            }
        }

        ExtendedDialog confirmDlg = new ExtendedDialog(Main.parent, tr("Confirm Purging"), new String[] {tr("Purge"), tr("Cancel")});
        confirmDlg.setContent(buildPanel(modified), false);
        confirmDlg.setButtonIcons(new String[] {"ok", "cancel"});

        int answer = confirmDlg.showDialog().getValue();
        if (answer != 1)
            return;

        Main.pref.put("purge.clear_undo_redo", cbClearUndoRedo.isSelected());

        Main.main.undoRedo.add(new PurgeCommand(Main.main.getEditLayer(), toPurgeChecked, makeIncomplete));

        if (cbClearUndoRedo.isSelected()) {
            Main.main.undoRedo.clean();
            getCurrentDataSet().clearSelectionHistory();
        }
    }

    private JPanel buildPanel(boolean modified) {
        JPanel pnl = new JPanel(new GridBagLayout());

        pnl.add(Box.createRigidArea(new Dimension(400,0)), GBC.eol().fill(GBC.HORIZONTAL));

        pnl.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        pnl.add(new JLabel("<html>"+
                tr("This operation makes JOSM forget the selected objects.<br> " +
                        "They will be removed from the layer, but <i>not</i> deleted<br> " +
                        "on the server when uploading.")+"</html>",
                        ImageProvider.get("purge"), JLabel.LEFT), GBC.eol().fill(GBC.HORIZONTAL));

        if (!toPurgeAdditionally.isEmpty()) {
            pnl.add(new JSeparator(), GBC.eol().fill(GBC.HORIZONTAL).insets(0,5,0,5));
            pnl.add(new JLabel("<html>"+
                    tr("The following dependent objects will be purged<br> " +
                            "in addition to the selected objects:")+"</html>",
                            ImageProvider.get("warning-small"), JLabel.LEFT), GBC.eol().fill(GBC.HORIZONTAL));

            Collections.sort(toPurgeAdditionally, new Comparator<OsmPrimitive>() {
                @Override
                public int compare(OsmPrimitive o1, OsmPrimitive o2) {
                    int type = o2.getType().compareTo(o1.getType());
                    if (type != 0)
                        return type;
                    return (Long.valueOf(o1.getUniqueId())).compareTo(o2.getUniqueId());
                }
            });
            JList list = new JList(toPurgeAdditionally.toArray(new OsmPrimitive[toPurgeAdditionally.size()]));
            /* force selection to be active for all entries */
            list.setCellRenderer(new OsmPrimitivRenderer() {
                @Override
                public Component getListCellRendererComponent(JList list,
                        Object value,
                        int index,
                        boolean isSelected,
                        boolean cellHasFocus) {
                    return super.getListCellRendererComponent(list, value, index, true, false);
                }
            });
            JScrollPane scroll = new JScrollPane(list);
            scroll.setPreferredSize(new Dimension(250, 300));
            scroll.setMinimumSize(new Dimension(250, 300));
            pnl.add(scroll, GBC.std().fill(GBC.VERTICAL).weight(0.0, 1.0));

            JButton addToSelection = new JButton(new AbstractAction() {
                {
                    putValue(SHORT_DESCRIPTION,   tr("Add to selection"));
                    putValue(SMALL_ICON, ImageProvider.get("dialogs","select"));
                }

                @Override
                public void actionPerformed(ActionEvent e) {
                    layer.data.addSelected(toPurgeAdditionally);
                }
            });
            addToSelection.setMargin(new Insets(0,0,0,0));
            pnl.add(addToSelection, GBC.eol().anchor(GBC.SOUTHWEST).weight(1.0, 1.0).insets(2,0,0,3));
        }

        if (modified) {
            pnl.add(new JSeparator(), GBC.eol().fill(GBC.HORIZONTAL).insets(0,5,0,5));
            pnl.add(new JLabel("<html>"+tr("Some of the objects are modified.<br> " +
                    "Proceed, if these changes should be discarded."+"</html>"),
                    ImageProvider.get("warning-small"), JLabel.LEFT),
                    GBC.eol().fill(GBC.HORIZONTAL));
        }

        cbClearUndoRedo = new JCheckBox(tr("Clear Undo/Redo buffer"));
        cbClearUndoRedo.setSelected(Main.pref.getBoolean("purge.clear_undo_redo", false));

        pnl.add(new JSeparator(), GBC.eol().fill(GBC.HORIZONTAL).insets(0,5,0,5));
        pnl.add(cbClearUndoRedo, GBC.eol());
        return pnl;
    }

    @Override
    protected void updateEnabledState() {
        if (getCurrentDataSet() == null) {
            setEnabled(false);
        } else {
            setEnabled(!(getCurrentDataSet().selectionEmpty()));
        }
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        setEnabled(selection != null && !selection.isEmpty());
    }

    private boolean hasOnlyIncompleteMembers(Relation r, Collection<OsmPrimitive> toPurge, Collection<? extends OsmPrimitive> moreToPurge) {
        for (RelationMember m : r.getMembers()) {
            if (!m.getMember().isIncomplete() && !toPurge.contains(m.getMember()) && !moreToPurge.contains(m.getMember()))
                return false;
        }
        return true;
    }
}
