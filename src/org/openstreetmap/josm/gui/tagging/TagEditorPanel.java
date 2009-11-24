// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionCache;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionList;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * TagEditorPanel is a {@see JPanel} which can be embedded as UI component in
 * UIs. It provides a spreadsheet like tabular control for editing tag names
 * and tag values. Two action buttons are placed on the left, one for additing
 * a new tag and one for deleting the currently selected tags.
 *
 *
 */
public class TagEditorPanel extends JPanel {
    /** the tag editor model */
    private TagEditorModel model;
    /** the tag table */
    private TagTable tagTable;

    private AutoCompletionCache acCache;
    private AutoCompletionList acList;

    /**
     * builds the panel with the table for editing tags
     *
     * @return the panel
     */
    protected JPanel buildTagTableEditorPanel() {

        JPanel pnl = new JPanel();
        model = new TagEditorModel();
        tagTable = new TagTable(model);

        pnl.setLayout(new BorderLayout());
        pnl.add(new JScrollPane(tagTable), BorderLayout.CENTER);
        return pnl;
    }

    /**
     * builds the panel with the button row
     *
     * @return the panel
     */
    protected JPanel buildButtonsPanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));

        // add action
        //
        AddAction addAction = new AddAction();
        pnl.add(new JButton(addAction));
        tagTable.addPropertyChangeListener(addAction);

        // delete action
        //
        DeleteAction deleteAction = new DeleteAction();
        tagTable.getSelectionModel().addListSelectionListener(deleteAction);
        tagTable.addPropertyChangeListener(deleteAction);
        pnl.add(new JButton(deleteAction));
        return pnl;
    }

    /**
     * builds the GUI
     */
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

    /**
     * constructor
     */
    public TagEditorPanel() {
        build();
    }

    /**
     * Replies the tag editor model used by this panel.
     *
     * @return the tag editor model used by this panel
     */
    public TagEditorModel getModel() {
        return model;
    }

    /**
     * The action for adding a tag
     *
     */
    class AddAction extends AbstractAction implements PropertyChangeListener {
        public AddAction() {
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "add"));
            putValue(SHORT_DESCRIPTION, tr("Add a new tag"));
            updateEnabledState();
        }

        public void actionPerformed(ActionEvent e) {
            model.appendNewTag();
        }

        protected void updateEnabledState() {
            setEnabled(tagTable.isEnabled());
        }

        public void propertyChange(PropertyChangeEvent evt) {
            updateEnabledState();
        }
    }

    /**
     * The action for deleting the currently selected tags
     *
     *
     */
    class DeleteAction extends AbstractAction implements ListSelectionListener, PropertyChangeListener {
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
                    throw new IllegalStateException("unexpected selected column: getSelectedColumn() is "
                            + tagTable.getSelectedColumn());
            } else if (tagTable.getSelectedColumnCount() == 2) {
                deleteTags();
            }
            if (model.getRowCount() == 0) {
                model.ensureOneTag();
            }
        }

        public void updateEnabledState() {
            setEnabled(tagTable.isEnabled() &&
                    (tagTable.getSelectedRowCount() > 0 || tagTable.getSelectedColumnCount() >0));
        }
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }

        public void propertyChange(PropertyChangeEvent evt) {
            updateEnabledState();
        }
    }

    public void initAutoCompletion(OsmDataLayer layer) {
        // initialize the autocompletion infrastructure
        //
        acCache = AutoCompletionCache.getCacheForLayer(layer);
        acCache.initFromDataSet();
        acList = new AutoCompletionList();

        TagCellEditor editor = ((TagCellEditor) tagTable.getColumnModel().getColumn(0).getCellEditor());
        editor.setAutoCompletionCache(acCache);
        editor.setAutoCompletionList(acList);
        editor = ((TagCellEditor) tagTable.getColumnModel().getColumn(1).getCellEditor());
        editor.setAutoCompletionCache(acCache);
        editor.setAutoCompletionList(acList);
    }

    @Override
    public void setEnabled(boolean enabled) {
        tagTable.setEnabled(enabled);
        super.setEnabled(enabled);
    }
}
