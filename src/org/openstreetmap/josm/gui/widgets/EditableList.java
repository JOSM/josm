// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.GBC;

/**
 * A {@link JList} containing items, and {@link JButton}s to add/edit/delete items.
 */
public class EditableList extends JPanel {

    /**
     * The title displayed in input dialog
     */
    public final String title;
    /**
     * The list items
     */
    public final JList<String> sourcesList = new JList<>(new DefaultListModel<String>());
    /**
     * The add button
     */
    public final JButton addSrcButton = new JButton(tr("Add"));
    /**
     * The edit button displayed nex to the list
     */
    public final JButton editSrcButton = new JButton(tr("Edit"));
    /**
     * The delete button
     */
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

        addSrcButton.addActionListener(e -> {
            String source = JOptionPane.showInputDialog(
                    MainApplication.getMainFrame(),
                    title,
                    title,
                    JOptionPane.QUESTION_MESSAGE);
            if (source != null && !source.isEmpty()) {
                ((DefaultListModel<String>) sourcesList.getModel()).addElement(source);
            }
            sourcesList.clearSelection();
        });

        editSrcButton.addActionListener(e -> {
            int row = sourcesList.getSelectedIndex();
            if (row == -1 && sourcesList.getModel().getSize() == 1) {
                sourcesList.setSelectedIndex(0);
                row = 0;
            }
            if (row == -1) {
                if (sourcesList.getModel().getSize() == 0) {
                    String source1 = JOptionPane.showInputDialog(MainApplication.getMainFrame(), title, title, JOptionPane.QUESTION_MESSAGE);
                    if (source1 != null && !source1.isEmpty()) {
                        ((DefaultListModel<String>) sourcesList.getModel()).addElement(source1);
                    }
                } else {
                    JOptionPane.showMessageDialog(
                            MainApplication.getMainFrame(),
                            tr("Please select the row to edit."),
                            tr("Information"),
                            JOptionPane.INFORMATION_MESSAGE
                    );
                }
            } else {
                String source2 = (String) JOptionPane.showInputDialog(MainApplication.getMainFrame(),
                        title,
                        title,
                        JOptionPane.QUESTION_MESSAGE, null, null,
                        sourcesList.getSelectedValue());
                if (source2 != null && !source2.isEmpty()) {
                    ((DefaultListModel<String>) sourcesList.getModel()).setElementAt(source2, row);
                }
            }
            sourcesList.clearSelection();
        });

        deleteSrcButton.addActionListener(e -> {
            if (sourcesList.getSelectedIndex() == -1) {
                JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("Please select the row to delete."), tr("Information"),
                        JOptionPane.QUESTION_MESSAGE);
            } else {
                ((DefaultListModel<String>) sourcesList.getModel()).remove(sourcesList.getSelectedIndex());
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

    /**
     * Sets the list items by a given list of strings
     * @param items The items that should be set
     */
    public void setItems(final Iterable<String> items) {
        for (String source : items) {
            ((DefaultListModel<String>) sourcesList.getModel()).addElement(source);
        }
    }

    /**
     * Gets all items that are currently displayed
     * @return All items as list of strings
     */
    public List<String> getItems() {
        return IntStream.range(0, sourcesList.getModel().getSize())
                .mapToObj(i -> sourcesList.getModel().getElementAt(i))
                .collect(Collectors.toList());
    }

    @Override
    public void setEnabled(boolean enabled) {
        sourcesList.setEnabled(enabled);
        addSrcButton.setEnabled(enabled);
        editSrcButton.setEnabled(enabled);
        deleteSrcButton.setEnabled(enabled);
    }
}
