// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.util.EventObject;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;

import org.openstreetmap.josm.actions.SaveActionBase;
import org.openstreetmap.josm.gui.widgets.JosmTextField;

/**
 * This is a {@link TableCellEditor} for filenames. It provides a text input field and
 * a button for launchinig a {@link javax.swing.JFileChooser}.
 */
class FilenameCellEditor extends JPanel implements TableCellEditor {
    private JosmTextField tfFileName;
    private CopyOnWriteArrayList<CellEditorListener> listeners;
    private File value;

    /**
     * build the GUI
     */
    protected void build() {
        setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        add(tfFileName = new JosmTextField(), gc);

        gc.gridx = 1;
        gc.gridy = 0;
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 0.0;
        gc.weighty = 1.0;
        add(new JButton(new LaunchFileChooserAction()));

        tfFileName.addFocusListener(
                new FocusAdapter() {
                    @Override
                    public void focusGained(FocusEvent e) {
                        tfFileName.selectAll();
                    }
                }
        );
    }

    public FilenameCellEditor() {
        listeners = new CopyOnWriteArrayList<CellEditorListener>();
        build();
    }

    @Override
    public void addCellEditorListener(CellEditorListener l) {
        if (l != null) {
            listeners.addIfAbsent(l);
        }
    }

    protected void fireEditingCanceled() {
        for (CellEditorListener l: listeners) {
            l.editingCanceled(new ChangeEvent(this));
        }
    }

    protected void fireEditingStopped() {
        for (CellEditorListener l: listeners) {
            l.editingStopped(new ChangeEvent(this));
        }
    }

    @Override
    public void cancelCellEditing() {
        fireEditingCanceled();
    }

    @Override
    public Object getCellEditorValue() {
        return value;
    }

    @Override
    public boolean isCellEditable(EventObject anEvent) {
        return true;
    }

    @Override
    public void removeCellEditorListener(CellEditorListener l) {
        listeners.remove(l);
    }

    @Override
    public boolean shouldSelectCell(EventObject anEvent) {
        return true;
    }

    @Override
    public boolean stopCellEditing() {
        if (tfFileName.getText() == null || tfFileName.getText().trim().isEmpty()) {
            value = null;
        } else {
            value = new File(tfFileName.getText());
        }
        fireEditingStopped();
        return true;
    }

    public void setInitialValue(File initialValue) {
        this.value = initialValue;
        if (initialValue == null) {
            this.tfFileName.setText("");
        } else {
            this.tfFileName.setText(initialValue.toString());
        }
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        SaveLayerInfo info = (SaveLayerInfo)value;
        setInitialValue(info.getFile());
        tfFileName.selectAll();
        return this;
    }

    class LaunchFileChooserAction extends AbstractAction {
        public LaunchFileChooserAction() {
            putValue(NAME, "...");
            putValue(SHORT_DESCRIPTION, tr("Launch a file chooser to select a file"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            File f = SaveActionBase.createAndOpenSaveFileChooser(tr("Select filename"), "osm");
            if (f != null) {
                FilenameCellEditor.this.tfFileName.setText(f.toString());
                FilenameCellEditor.this.tfFileName.selectAll();
            }
        }
    }
}
