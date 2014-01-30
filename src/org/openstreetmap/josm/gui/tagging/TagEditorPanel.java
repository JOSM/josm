// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.EnumSet;
import javax.swing.AbstractAction;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.openstreetmap.josm.gui.dialogs.properties.PresetListPanel;
import org.openstreetmap.josm.gui.dialogs.properties.PresetListPanel.PresetHandler;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionList;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionManager;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * TagEditorPanel is a {@link JPanel} which can be embedded as UI component in
 * UIs. It provides a spreadsheet like tabular control for editing tag names
 * and tag values. Two action buttons are placed on the left, one for adding
 * a new tag and one for deleting the currently selected tags.
 *
 */
public class TagEditorPanel extends JPanel {
    /** the tag editor model */
    private TagEditorModel model;
    /** the tag table */
    private TagTable tagTable;

    private PresetListPanel presetListPanel;
    private final PresetHandler presetHandler;

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
        if (presetHandler != null) {
            presetListPanel = new PresetListPanel();
            pnl.add(presetListPanel, BorderLayout.NORTH);
        }
        return pnl;
    }

    public void setNextFocusComponent(Component nextFocusComponent) {
        tagTable.setNextFocusComponent(nextFocusComponent);
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
        
        // paste action
        pnl.add(btn = new JButton(tagTable.getPasteAction()));
        btn.setMargin(new Insets(0,0,0,0));
        tagTable.addComponentNotStoppingCellEditing(btn);
        return pnl;
    }

    public AbstractAction getPasteAction() {
        return tagTable.getPasteAction();
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

        if (presetHandler != null) {
            model.addTableModelListener(new TableModelListener() {
                @Override
                public void tableChanged(TableModelEvent e) {
                    updatePresets();
                }
            });
        }

        addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                tagTable.requestFocusInCell(0, 0);
            }
        });
    }

    /**
     * Creates a new tag editor panel. The editor model is created
     * internally and can be retrieved with {@link #getModel()}.
     */
    public TagEditorPanel(PresetHandler presetHandler) {
        this(null, presetHandler);
    }

    /**
     * Creates a new tag editor panel with a supplied model. If
     * {@code model} is null, a new model is created.
     *
     * @param model the tag editor model
     */
    public TagEditorPanel(TagEditorModel model, PresetHandler presetHandler) {
        this.model = model;
        this.presetHandler = presetHandler;
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

        AutoCompletionManager autocomplete = layer.data.getAutoCompletionManager();
        AutoCompletionList acList = new AutoCompletionList();

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

    private void updatePresets() {
        presetListPanel.updatePresets(
                EnumSet.of(TaggingPresetType.RELATION),
                model.getTags(), presetHandler);
        validate();
    }
}
