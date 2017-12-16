// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.PurgeCommand;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.ConditionalOptionPaneUtil;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.OsmPrimitivRenderer;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.spi.preferences.Config;
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
 * undo buffer, we must re-add the identical object (and not semantically equal ones).
 *
 * @since 3431
 */
public class PurgeAction extends JosmAction {

    protected transient OsmDataLayer layer;
    protected JCheckBox cbClearUndoRedo;
    protected boolean modified;

    /**
     * Subset of toPurgeChecked. Those that have not been in the selection.
     */
    protected transient List<OsmPrimitive> toPurgeAdditionally;

    /**
     * Constructs a new {@code PurgeAction}.
     */
    public PurgeAction() {
        /* translator note: other expressions for "purge" might be "forget", "clean", "obliterate", "prune" */
        super(tr("Purge..."), "purge", tr("Forget objects but do not delete them on server when uploading."),
                Shortcut.registerShortcut("system:purge", tr("Edit: {0}", tr("Purge")), KeyEvent.VK_P, Shortcut.CTRL_SHIFT),
                true);
        putValue("help", HelpUtil.ht("/Action/Purge"));
    }

    /** force selection to be active for all entries */
    static class SelectionForcedOsmPrimitivRenderer extends OsmPrimitivRenderer {
        @Override
        public Component getListCellRendererComponent(JList<? extends OsmPrimitive> list,
                OsmPrimitive value, int index, boolean isSelected, boolean cellHasFocus) {
            return super.getListCellRendererComponent(list, value, index, true, false);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;

        PurgeCommand cmd = getPurgeCommand(getLayerManager().getEditDataSet().getAllSelected());
        boolean clearUndoRedo = false;

        if (!GraphicsEnvironment.isHeadless()) {
            final boolean answer = ConditionalOptionPaneUtil.showConfirmationDialog(
                    "purge", Main.parent, buildPanel(modified), tr("Confirm Purging"),
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_OPTION);
            if (!answer)
                return;

            clearUndoRedo = cbClearUndoRedo.isSelected();
            Config.getPref().putBoolean("purge.clear_undo_redo", clearUndoRedo);
        }

        MainApplication.undoRedo.add(cmd);
        if (clearUndoRedo) {
            MainApplication.undoRedo.clean();
            getLayerManager().getEditDataSet().clearSelectionHistory();
        }
    }

    /**
     * Creates command to purge selected OSM primitives.
     * @param sel selected OSM primitives
     * @return command to purge selected OSM primitives
     * @since 11252
     */
    public PurgeCommand getPurgeCommand(Collection<OsmPrimitive> sel) {
        layer = getLayerManager().getEditLayer();
        toPurgeAdditionally = new ArrayList<>();
        PurgeCommand cmd = PurgeCommand.build(sel, toPurgeAdditionally);
        modified = cmd.getParticipatingPrimitives().stream().anyMatch(OsmPrimitive::isModified);
        return cmd;
    }

    private JPanel buildPanel(boolean modified) {
        JPanel pnl = new JPanel(new GridBagLayout());

        pnl.add(Box.createRigidArea(new Dimension(400, 0)), GBC.eol().fill(GBC.HORIZONTAL));

        pnl.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        pnl.add(new JLabel("<html>"+
                tr("This operation makes JOSM forget the selected objects.<br> " +
                        "They will be removed from the layer, but <i>not</i> deleted<br> " +
                        "on the server when uploading.")+"</html>",
                        ImageProvider.get("purge"), JLabel.LEFT), GBC.eol().fill(GBC.HORIZONTAL));

        if (!toPurgeAdditionally.isEmpty()) {
            pnl.add(new JSeparator(), GBC.eol().fill(GBC.HORIZONTAL).insets(0, 5, 0, 5));
            pnl.add(new JLabel("<html>"+
                    tr("The following dependent objects will be purged<br> " +
                            "in addition to the selected objects:")+"</html>",
                            ImageProvider.get("warning-small"), JLabel.LEFT), GBC.eol().fill(GBC.HORIZONTAL));

            toPurgeAdditionally.sort((o1, o2) -> {
                int type = o2.getType().compareTo(o1.getType());
                if (type != 0)
                    return type;
                return Long.compare(o1.getUniqueId(), o2.getUniqueId());
            });
            JList<OsmPrimitive> list = new JList<>(toPurgeAdditionally.toArray(new OsmPrimitive[0]));
            /* force selection to be active for all entries */
            list.setCellRenderer(new SelectionForcedOsmPrimitivRenderer());
            JScrollPane scroll = new JScrollPane(list);
            scroll.setPreferredSize(new Dimension(250, 300));
            scroll.setMinimumSize(new Dimension(250, 300));
            pnl.add(scroll, GBC.std().fill(GBC.BOTH).weight(1.0, 1.0));

            JButton addToSelection = new JButton(new AbstractAction() {
                {
                    putValue(SHORT_DESCRIPTION, tr("Add to selection"));
                    new ImageProvider("dialogs", "select").getResource().attachImageIcon(this, true);
                }

                @Override
                public void actionPerformed(ActionEvent e) {
                    layer.data.addSelected(toPurgeAdditionally);
                }
            });
            addToSelection.setMargin(new Insets(0, 0, 0, 0));
            pnl.add(addToSelection, GBC.eol().anchor(GBC.SOUTHWEST).weight(0.0, 1.0).insets(2, 0, 0, 3));
        }

        if (modified) {
            pnl.add(new JSeparator(), GBC.eol().fill(GBC.HORIZONTAL).insets(0, 5, 0, 5));
            pnl.add(new JLabel("<html>"+tr("Some of the objects are modified.<br> " +
                    "Proceed, if these changes should be discarded."+"</html>"),
                    ImageProvider.get("warning-small"), JLabel.LEFT),
                    GBC.eol().fill(GBC.HORIZONTAL));
        }

        cbClearUndoRedo = new JCheckBox(tr("Clear Undo/Redo buffer"));
        cbClearUndoRedo.setSelected(Config.getPref().getBoolean("purge.clear_undo_redo", false));

        pnl.add(new JSeparator(), GBC.eol().fill(GBC.HORIZONTAL).insets(0, 5, 0, 5));
        pnl.add(cbClearUndoRedo, GBC.eol());
        return pnl;
    }

    @Override
    protected void updateEnabledState() {
        DataSet ds = getLayerManager().getEditDataSet();
        setEnabled(ds != null && !ds.selectionEmpty());
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        setEnabled(selection != null && !selection.isEmpty());
    }
}
