// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractCellEditor;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

/**
 * Class based on:  http://www.camick.com/java/source/ButtonColumn.java
 * https://tips4java.wordpress.com/2009/07/12/table-button-column/
 * @since 10536
 */
public class ButtonColumn extends AbstractCellEditor implements TableCellRenderer, TableCellEditor, ActionListener {
    private final Action action;
    private final JButton renderButton;
    private final JButton editButton;
    private Object editorValue;
    private String buttonName;

    /**
     * Creates a column that is rendered as a button with no action bound to the click event
     */
    public ButtonColumn() {
        this(null);
    }

    /**
     * Constructs a new {@code ButtonColumn}.
     * @param action action
     * @param buttonName button name
     */
    public ButtonColumn(Action action, String buttonName) {
        this(action);
        this.buttonName = buttonName;
    }

    /**
     * Creates a column that is rendered as a button
     *
     * @param action action to be performed when button is pressed
     */
    public ButtonColumn(Action action) {
        this.action = action;
        renderButton = new JButton();
        editButton = new JButton();
        editButton.setFocusPainted(false);
        editButton.addActionListener(this);
        editButton.setBorder(new LineBorder(Color.BLUE));
    }

    @Override
    public Object getCellEditorValue() {
        return editorValue;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        this.action.actionPerformed(e);
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        this.editorValue = value;
        if (buttonName != null) {
            editButton.setText(buttonName);
            editButton.setIcon(null);
        } else if (value == null) {
            editButton.setText("");
            editButton.setIcon(null);
        } else if (value instanceof Icon) {
            editButton.setText("");
            editButton.setIcon((Icon) value);
        } else {
            editButton.setText(value.toString());
            editButton.setIcon(null);
        }
        return editButton;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        if (isSelected) {
            renderButton.setForeground(table.getSelectionForeground());
            renderButton.setBackground(table.getSelectionBackground());
        } else {
            renderButton.setForeground(table.getForeground());
            renderButton.setBackground(UIManager.getColor("Button.background"));
        }

        renderButton.setFocusPainted(hasFocus);

        if (buttonName != null) {
            renderButton.setText(buttonName);
            renderButton.setIcon(null);
        } else if (value == null) {
            renderButton.setText("");
            renderButton.setIcon(null);
        } else if (value instanceof Icon) {
            renderButton.setText("");
            renderButton.setIcon((Icon) value);
        } else {
            renderButton.setText(value.toString());
            renderButton.setIcon(null);
        }
        return renderButton;
    }
}
