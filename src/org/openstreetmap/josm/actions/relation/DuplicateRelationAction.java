// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.relation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.ConditionalOptionPaneUtil;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditor;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Creates a new relation with a copy of the current editor state
 * @since 5799
 */
public class DuplicateRelationAction extends AbstractRelationAction {

    /**
     * Constructs a new {@code DuplicateRelationAction}.
     */
    public DuplicateRelationAction() {
        putValue(SHORT_DESCRIPTION, tr("Create a copy of this relation and open it in another editor window"));
        new ImageProvider("duplicate").getResource().attachImageIcon(this, true);
        putValue(NAME, tr("Duplicate"));
    }

    /**
     * Duplicates the given relation and launches the relation editor for the created copy.
     * @param original The relation to duplicate
     */
    public static void duplicateRelationAndLaunchEditor(Relation original) {
        if (!confirmRelationDuplicate(original)) {
            return;
        }
        Relation copy = new Relation(original, true);
        copy.setModified(true);
        RelationEditor editor = RelationEditor.getEditor(
                MainApplication.getLayerManager().getEditLayer(),
                copy,
                null /* no selected members */
                );
        editor.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled() || relations.isEmpty())
            return;
        IRelation<?> r = relations.iterator().next();
        if (r instanceof Relation) {
            duplicateRelationAndLaunchEditor((Relation) r);
        }
    }

    private static boolean isEditableRelation(IRelation<?> r) {
        return r instanceof Relation && r.getDataSet() != null && !r.getDataSet().isLocked();
    }

    @Override
    protected void updateEnabledState() {
        // only one selected relation can be edited
        setEnabled(relations.size() == 1
                && isEditableRelation(relations.iterator().next()));
    }

    private static boolean confirmRelationDuplicate(Relation relation) {
        JPanel msg = new JPanel(new GridBagLayout());
        msg.add(new JMultilineLabel("<html>" + tr(
                "You are about to duplicate {0} relation: {1}"
                        + "<br/>"
                        + "This step is rarely necessary. Do you really want to duplicate?",
                1, DefaultNameFormatter.getInstance().formatAsHtmlUnorderedList(relation))
                + "</html>"));
        return ConditionalOptionPaneUtil.showConfirmationDialog(
                "delete_relations",
                MainApplication.getMainFrame(),
                msg,
                tr("Duplicate relation?"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                JOptionPane.YES_OPTION);
    }
}
