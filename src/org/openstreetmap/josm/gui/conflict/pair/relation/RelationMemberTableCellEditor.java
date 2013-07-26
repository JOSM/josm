// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.relation;

import java.awt.Component;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.widgets.JosmTextField;


/**
 * {@link TableCellEditor} for the role column in a table for {@link RelationMember}s.
 *
 */
public class RelationMemberTableCellEditor extends AbstractCellEditor implements TableCellEditor{

    private final JosmTextField editor;

    /**
     * Constructs a new {@code RelationMemberTableCellEditor}.
     */
    public RelationMemberTableCellEditor() {
        editor = new JosmTextField();
        editor.addFocusListener(
                new FocusAdapter() {
                    @Override
                    public void focusGained(FocusEvent arg0) {
                        editor.selectAll();
                    }
                }
        );
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        // Do not edit empty or incomplete members ! (fix #5374 and #6315)
        if (value == null)
            return null;

        RelationMember member = (RelationMember)value;

        editor.setText(member.getRole());
        editor.selectAll();
        return editor;
    }

    @Override
    public Object getCellEditorValue() {
        return editor.getText();
    }

}
