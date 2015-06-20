// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.GBC;

/**
 * A {@link JList} containing items, and {@link JButton}s to add/edit/delete items.
 */
public class EditableList extends JPanel {

    public final String title;
    public final JList<String> sourcesList = new JList<>(new DefaultListModel<String>());
    public final JButton addSrcButton = new JButton(tr("Add"));
    public final JButton editSrcButton = new JButton(tr("Edit"));
    public final JButton deleteSrcButton = new JButton(tr("Delete"));

    /**
     * Constructs a new {@code EditableList}.
     * @param title The title displayed in input dialog
     */
    public EditableList(String title) {
        this.title = title;
        build();
    }

    protected final void build() {

        setLayout(new BorderLayout());

        addSrcButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String source = JOptionPane.showInputDialog(
                        Main.parent,
                        title,
                        title,
                        JOptionPane.QUESTION_MESSAGE);
                if (source != null) {
                    ((DefaultListModel<String>) sourcesList.getModel()).addElement(source);
                }
                sourcesList.clearSelection();
            }
        });

        editSrcButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = sourcesList.getSelectedIndex();
                if (row == -1 && sourcesList.getModel().getSize() == 1) {
                    sourcesList.setSelectedIndex(0);
                    row = 0;
                }
                if (row == -1) {
                    if (sourcesList.getModel().getSize() == 0) {
                        String source = JOptionPane.showInputDialog(Main.parent, title, title, JOptionPane.QUESTION_MESSAGE);
                        if (source != null) {
                            ((DefaultListModel<String>) sourcesList.getModel()).addElement(source);
                        }
                    } else {
                        JOptionPane.showMessageDialog(
                                Main.parent,
                                tr("Please select the row to edit."),
                                tr("Information"),
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    }
                } else {
                    String source = (String) JOptionPane.showInputDialog(Main.parent,
                            title,
                            title,
                            JOptionPane.QUESTION_MESSAGE, null, null,
                            sourcesList.getSelectedValue());
                    if (source != null) {
                        ((DefaultListModel<String>) sourcesList.getModel()).setElementAt(source, row);
                    }
                }
                sourcesList.clearSelection();
            }
        });

        deleteSrcButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (sourcesList.getSelectedIndex() == -1) {
                    JOptionPane.showMessageDialog(Main.parent, tr("Please select the row to delete."), tr("Information"),
                            JOptionPane.QUESTION_MESSAGE);
                } else {
                    ((DefaultListModel<String>) sourcesList.getModel()).remove(sourcesList.getSelectedIndex());
                }
            }
        });
        sourcesList.setMinimumSize(new Dimension(300, 50));
        sourcesList.setVisibleRowCount(3);

        addSrcButton.setToolTipText(tr("Add a new source to the list."));
        editSrcButton.setToolTipText(tr("Edit the selected source."));
        deleteSrcButton.setToolTipText(tr("Delete the selected source from the list."));

        final JPanel buttonPanel = new JPanel(new GridBagLayout());
        buttonPanel.add(addSrcButton, GBC.std().insets(0, 5, 0, 0));
        buttonPanel.add(editSrcButton, GBC.std().insets(5, 5, 5, 0));
        buttonPanel.add(deleteSrcButton, GBC.std().insets(0, 5, 0, 0));

        add(new JScrollPane(sourcesList), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        setPreferredSize(new Dimension(300, 50 + (int) buttonPanel.getPreferredSize().getHeight()));

    }

    public void setItems(final Iterable<String> items) {
        for (String source : items) {
            ((DefaultListModel<String>) sourcesList.getModel()).addElement(source);
        }
    }

    public List<String> getItems() {
        final List<String> items = new ArrayList<>(sourcesList.getModel().getSize());
        for (int i = 0; i < sourcesList.getModel().getSize(); ++i) {
            items.add(sourcesList.getModel().getElementAt(i));
        }
        return items;
    }

    @Override
    public void setEnabled(boolean enabled) {
        sourcesList.setEnabled(enabled);
        addSrcButton.setEnabled(enabled);
        editSrcButton.setEnabled(enabled);
        deleteSrcButton.setEnabled(enabled);
    }
}
