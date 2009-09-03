// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.tools.ImageProvider;

public class TagEditorPanel extends JPanel {
    private TagEditorModel model;
    private TagTable tagTable;

    protected JPanel buildTagTableEditorPanel() {
        JPanel pnl = new JPanel();
        model = new TagEditorModel();
        tagTable = new TagTable(model);

        pnl.setLayout(new BorderLayout());
        pnl.add(new JScrollPane(tagTable), BorderLayout.CENTER);
        return pnl;
    }

    protected JPanel buildButtonsPanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));

        // add action
        //
        AddAction addAction = new AddAction();
        pnl.add(new JButton(addAction));

        // delete action
        //
        DeleteAction deleteAction = new DeleteAction();
        tagTable.getSelectionModel().addListSelectionListener(deleteAction);
        pnl.add(new JButton(deleteAction));
        return pnl;
    }

    protected void build() {
        setLayout(new GridBagLayout());
        JPanel tablePanel = buildTagTableEditorPanel();
        JPanel buttonPanel = buildButtonsPanel();

        GridBagConstraints gc = new GridBagConstraints();

        // -- buttons panel
        //
        gc.fill = GridBagConstraints.VERTICAL;
        gc.weightx = 0.0;
        gc.weighty = 1.0;
        gc.anchor = GridBagConstraints.NORTHWEST;
        add(buttonPanel,gc);

        // -- the panel with the editor table
        //
        gc.gridx = 1;
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        gc.anchor = GridBagConstraints.CENTER;
        add(tablePanel,gc);
    }
    public TagEditorPanel() {
        build();
    }

    public TagEditorModel getModel() {
        return model;
    }

    class AddAction extends AbstractAction  {
        public AddAction() {
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "add"));
            putValue(SHORT_DESCRIPTION, tr("Add a new tag"));
        }
        public void actionPerformed(ActionEvent e) {
            model.appendNewTag();
        }
    }

    class DeleteAction extends AbstractAction implements ListSelectionListener {
        public DeleteAction() {
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "delete"));
            putValue(SHORT_DESCRIPTION, tr("Delete the selection in the tag table"));
            updateEnabledState();
        }

        public void actionPerformed(ActionEvent e) {
            run();
        }

        /**
         * delete a selection of tag names
         */
        protected void deleteTagNames() {
            int[] rows = tagTable.getSelectedRows();
            model.deleteTagNames(rows);
        }

        /**
         * delete a selection of tag values
         */
        protected void deleteTagValues() {
            int[] rows = tagTable.getSelectedRows();
            model.deleteTagValues(rows);
        }

        /**
         * delete a selection of tags
         */
        protected void deleteTags() {
            model.deleteTags(tagTable.getSelectedRows());
        }

        public void run() {
            if (!isEnabled())
                return;
            if (tagTable.getSelectedColumnCount() == 1) {
                if (tagTable.getSelectedColumn() == 0) {
                    deleteTagNames();
                } else if (tagTable.getSelectedColumn() == 1) {
                    deleteTagValues();
                } else
                    // should not happen
                    //
                    throw new IllegalStateException("unexpected selected clolumn: getSelectedColumn() is "
                            + tagTable.getSelectedColumn());
            } else if (tagTable.getSelectedColumnCount() == 2) {
                deleteTags();
            }
            if (model.getRowCount() == 0) {
                model.ensureOneTag();
            }
        }

        public void updateEnabledState() {
            setEnabled(tagTable.getSelectedRowCount() > 0 || tagTable.getSelectedColumnCount() >0);
        }
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }
    }
}
