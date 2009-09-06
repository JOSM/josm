// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.tagging.AutoCompletingTextField;
import org.openstreetmap.josm.tools.ImageProvider;

public class RelationMemberConflictResolver extends JPanel {

    private AutoCompletingTextField tfRole;
    private AutoCompletingTextField tfKey;
    private AutoCompletingTextField tfValue;
    private JCheckBox cbTagRelations;
    private RelationMemberConflictResolverModel model;

    protected void build() {
        setLayout(new BorderLayout());
        model=new RelationMemberConflictResolverModel();
        add (new JScrollPane(new RelationMemberConflictResolverTable(model)), BorderLayout.CENTER);
        JPanel pnl = new JPanel();
        pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));
        pnl.add(buildRoleEditingPanel());
        pnl.add(buildTagRelationsPanel());
        add(pnl, BorderLayout.SOUTH);
    }

    protected JPanel buildRoleEditingPanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new FlowLayout(FlowLayout.LEFT));
        pnl.add(new JLabel(tr("Role:")));
        pnl.add(tfRole = new AutoCompletingTextField(10));
        pnl.add(new JButton(new ApplyRoleAction()));
        return pnl;
    }

    protected JPanel buildTagRelationsPanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new FlowLayout(FlowLayout.LEFT));
        cbTagRelations = new JCheckBox(tr("Tag modified relations with   "));
        cbTagRelations.addChangeListener(new ToggleTagRelationsAction());
        pnl.add(cbTagRelations);
        pnl.add(new JLabel(tr("Key:")));
        pnl.add(tfKey = new AutoCompletingTextField(10));
        pnl.add(new JLabel(tr("Value:")));
        pnl.add(tfValue = new AutoCompletingTextField(10));
        cbTagRelations.setSelected(false);
        tfKey.setEnabled(false);
        tfValue.setEnabled(false);
        return pnl;
    }

    public RelationMemberConflictResolver() {
        build();
    }

    class ApplyRoleAction extends AbstractAction {
        public ApplyRoleAction() {
            putValue(NAME, tr("Apply"));
            putValue(SMALL_ICON, ImageProvider.get("ok"));
            putValue(SHORT_DESCRIPTION, tr("Apply this role to all members"));
        }

        public void actionPerformed(ActionEvent e) {
            model.applyRole(tfRole.getText());
        }
    }

    class ToggleTagRelationsAction implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            ButtonModel buttonModel = ((AbstractButton)e.getSource()).getModel();
            tfKey.setEnabled(buttonModel.isSelected());
            tfValue.setEnabled(buttonModel.isSelected());
            tfKey.setBackground(buttonModel.isSelected() ? UIManager.getColor("TextField.background") : UIManager.getColor("Panel.background"));
            tfValue.setBackground(buttonModel.isSelected() ? UIManager.getColor("TextField.background") : UIManager.getColor("Panel.background"));
        }
    }

    public RelationMemberConflictResolverModel getModel() {
        return model;
    }

    public Command buildTagApplyCommands(Collection<? extends OsmPrimitive> primitives) {
        if (! cbTagRelations.isSelected()) return null;
        if (tfKey.getText().trim().equals("")) return null;
        if (tfValue.getText().trim().equals("")) return null;
        if (primitives == null || primitives.isEmpty()) return null;
        return new ChangePropertyCommand(primitives, tfKey.getText(), tfValue.getText());
    }
}
