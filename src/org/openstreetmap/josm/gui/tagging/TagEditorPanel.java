// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionList;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionManager;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * TagEditorPanel is a {@see JPanel} which can be embedded as UI component in
 * UIs. It provides a spreadsheet like tabular control for editing tag names
 * and tag values. Two action buttons are placed on the left, one for adding
 * a new tag and one for deleting the currently selected tags.
 *
 */
public class TagEditorPanel extends JPanel {
    @SuppressWarnings("unused")
    static private final Logger logger = Logger.getLogger(TagEditorPanel.class.getName());
    /** the tag editor model */
    private TagEditorModel model;
    /** the tag table */
    private TagTable tagTable;

    private AutoCompletionManager autocomplete;
    private AutoCompletionList acList;

    /**
     * builds the panel with the table for editing tags
     *
     * @return the panel
     */
    protected JPanel buildTagTableEditorPanel() {
        JPanel pnl = new JPanel();
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
        JButton btn;
        pnl.add(btn = new JButton(tagTable.getAddAction()));
        btn.setMargin(new Insets(0,0,0,0));
        tagTable.addComponentNotStoppingCellEditing(btn);

        // delete action
        pnl.add(btn = new JButton(tagTable.getDeleteAction()));
        btn.setMargin(new Insets(0,0,0,0));
        tagTable.addComponentNotStoppingCellEditing(btn);
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
     * Creates a new tag editor panel. The editor model is created
     * internally and can be retrieved with {@see #getModel()}.
     */
    public TagEditorPanel() {
        this(null);
    }

    /**
     * Creates a new tag editor panel with a supplied model. If
     * {@code model} is null, a new model is created.
     * 
     * @param model the tag editor model
     */
    public TagEditorPanel(TagEditorModel model) {
        this.model = model;
        if (this.model == null) {
            this.model = new TagEditorModel();
        }
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
     * Initializes the auto completion infrastructure used in this
     * tag editor panel. {@code layer} is the data layer from whose data set
     * tag values are proposed as auto completion items.
     * 
     * @param layer the data layer. Must not be null.
     * @throws IllegalArgumentException thrown if {@code layer} is null
     */
    public void initAutoCompletion(OsmDataLayer layer) throws IllegalArgumentException{
        CheckParameterUtil.ensureParameterNotNull(layer, "layer");

        autocomplete = layer.data.getAutoCompletionManager();
        acList = new AutoCompletionList();

        TagCellEditor editor = ((TagCellEditor) tagTable.getColumnModel().getColumn(0).getCellEditor());
        editor.setAutoCompletionManager(autocomplete);
        editor.setAutoCompletionList(acList);
        editor = ((TagCellEditor) tagTable.getColumnModel().getColumn(1).getCellEditor());
        editor.setAutoCompletionManager(autocomplete);
        editor.setAutoCompletionList(acList);
    }

    @Override
    public void setEnabled(boolean enabled) {
        tagTable.setEnabled(enabled);
        super.setEnabled(enabled);
    }
}
